package com.financas.app.data.service

import android.app.Notification
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.financas.app.FinancasApplication
import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.domain.usecase.BankNotificationParser
import com.financas.app.domain.usecase.DeduplicationResult
import com.financas.app.domain.usecase.FinancialAppFilter
import com.financas.app.domain.usecase.SmartPrefillEngine
import com.financas.app.domain.usecase.TransactionDeduplicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class FinancialNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val parser = BankNotificationParser()
    private val deduplicator = TransactionDeduplicator()

    override fun onListenerConnected() {
        super.onListenerConnected()
        android.util.Log.d("FinflowNotif", "FinancialNotificationListenerService connected and listening")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        android.util.Log.d("FinflowNotif", "FinancialNotificationListenerService disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return // Ignore self notifications
        if (!FinancialAppFilter.isFinancialApp(pkg)) return

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_CAPTURE_ENABLED, true)
        if (!isEnabled) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Ignore ongoing notifications like media playback or foreground services
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val ticker = notification.tickerText?.toString()

        val effectiveTitle = bigTitle?.takeIf { it.isNotBlank() } ?: title
        val effectiveText = when {
            !bigText.isNullOrBlank() && !text.isNullOrBlank() && !bigText.contains(text) -> "$text - $bigText"
            !bigText.isNullOrBlank() -> bigText
            !text.isNullOrBlank() -> text
            !ticker.isNullOrBlank() -> ticker
            else -> null
        }

        val parsed = parser.parse(
            packageName = pkg,
            title = effectiveTitle,
            text = effectiveText,
            subText = subText
        ) ?: return

        serviceScope.launch {
            processParsedNotification(
                amountCents = parsed.amountCents,
                merchant = parsed.merchant,
                paymentMethod = parsed.paymentMethod,
                sourceApp = parsed.sourceApp,
                direction = parsed.direction
            )
        }
    }

    private suspend fun processParsedNotification(
        amountCents: Long,
        merchant: String,
        paymentMethod: String,
        sourceApp: String,
        direction: String
    ) {
        val application = application as FinancasApplication
        val db = application.container.database
        val pendingDao = db.pendingTransactionDao()
        val transactionDao = db.transactionDao()
        val categoryDao = db.categoryDao()

        val now = LocalDate.now()
        val todayEpochDay = now.toEpochDay()
        val currentYearMonth = now.year * 100 + now.monthValue
        val nowTimestamp = System.currentTimeMillis()

        val existingPending = pendingDao.getAllSync()
        val monthFixed = transactionDao.getFixedTransactionsForMonth(currentYearMonth)
        val todayConfirmed = transactionDao.getByMonthSync(currentYearMonth).filter { it.epochDay == todayEpochDay }

        val evalResult = deduplicator.evaluate(
            incomingAmountCents = amountCents,
            incomingMerchant = merchant,
            incomingSourceApp = sourceApp,
            incomingTimestamp = nowTimestamp,
            todayEpochDay = todayEpochDay,
            currentYearMonth = currentYearMonth,
            existingPending = existingPending,
            monthFixedTransactions = monthFixed,
            todayConfirmedTransactions = todayConfirmed
        )

        when (evalResult) {
            is DeduplicationResult.DuplicateDrop -> {
                // Safely dropped to prevent multi-app alert duplicates
                return
            }
            is DeduplicationResult.MatchesExistingFixed -> {
                val pending = PendingTransactionEntity(
                    amountCents = amountCents,
                    rawMerchant = merchant,
                    suggestedType = direction,
                    suggestedCategoryId = evalResult.suggestedCategoryId,
                    suggestedContextTag = evalResult.suggestedContextTag,
                    suggestedPaymentMethod = paymentMethod,
                    isFixed = true,
                    sourceApp = sourceApp,
                    detectedEpochDay = todayEpochDay,
                    receivedTimestamp = nowTimestamp,
                    notes = "Corresponde ao fixo '${evalResult.matchedFixedTransaction.merchant}'"
                )
                pendingDao.insert(pending)
            }
            is DeduplicationResult.NewUnique -> {
                val allCategories = categoryDao.getAllSync()
                val history = transactionDao.getAllSync()
                val prefillEngine = SmartPrefillEngine(transactionDao)
                val suggestion = prefillEngine.findPrefill(merchant, history)

                val suggestedCategory = suggestion?.categoryId
                    ?: allCategories.firstOrNull { it.id == "sub_groceries" }?.id
                    ?: allCategories.firstOrNull { it.type == "EXPENSE" && it.parentId != null }?.id
                    ?: "sub_groceries"

                val suggestedContext = suggestion?.contextTag ?: "COMERCIO"

                val pending = PendingTransactionEntity(
                    amountCents = amountCents,
                    rawMerchant = merchant,
                    suggestedType = direction,
                    suggestedCategoryId = suggestedCategory,
                    suggestedContextTag = suggestedContext,
                    suggestedPaymentMethod = paymentMethod,
                    isFixed = false,
                    sourceApp = sourceApp,
                    detectedEpochDay = todayEpochDay,
                    receivedTimestamp = nowTimestamp
                )
                pendingDao.insert(pending)
            }
        }
    }

    companion object {
        const val PREFS_NAME = "bank_notification_capture_prefs"
        const val KEY_CAPTURE_ENABLED = "capture_enabled"

        fun isNotificationAccessGranted(context: Context): Boolean {
            return androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }

        fun requestRebindIfGranted(context: Context) {
            if (isNotificationAccessGranted(context)) {
                try {
                    val component = android.content.ComponentName(context, FinancialNotificationListenerService::class.java)
                    requestRebind(component)
                } catch (_: Exception) {}
            }
        }

        fun isCaptureEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_CAPTURE_ENABLED, true)
        }

        fun setCaptureEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_CAPTURE_ENABLED, enabled)
                .apply()
        }
    }
}
