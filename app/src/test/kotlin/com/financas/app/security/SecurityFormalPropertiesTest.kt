package com.financas.app.security

import com.financas.app.core.security.DatabaseKeyManager
import com.financas.app.core.security.LocalFileCipher
import com.financas.app.core.security.SessionBackgroundClock
import com.financas.app.core.security.SessionLockPolicy
import com.financas.app.data.network.SafeHttp
import com.financas.app.data.repository.BackupCrypto
import com.financas.app.data.repository.BackupPasswordPolicy
import com.financas.app.data.repository.WrongBackupPasswordException
import com.financas.app.domain.usecase.BankNotificationParser
import com.financas.app.domain.usecase.FinancialAppFilter
import com.financas.app.domain.usecase.PendingFloodGuard
import com.financas.app.domain.usecase.UpdateSignatureCheck
import com.financas.app.domain.usecase.UpdateUrlPolicy
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.KeyGenerator

class SecurityFormalPropertiesTest {

    private val fastIterations = 100_000

    @Test
    fun backupCrypto_roundTripAndNonDeterminism_acrossRandomizedInputs() {
        val rng = SecureRandom(ByteArray(16) { it.toByte() })
        val alphabet = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#\$%&*?"
        val payloads = listOf(
            "",
            "{}",
            "{\"merchant\":\"Café São João — 12,50 €\"}",
            "A".repeat(4096)
        ) + List(10) { idx ->
            val len = 1 + rng.nextInt(512)
            buildString(len) {
                repeat(len) { append(alphabet[rng.nextInt(alphabet.length)]) }
            } + "_$idx"
        }

        for (plain in payloads) {
            val pw = buildString(14) {
                repeat(14) { append(alphabet[rng.nextInt(alphabet.length)]) }
            }
            val enc1 = BackupCrypto.encrypt(plain, pw.toCharArray(), iterations = fastIterations)
            val enc2 = BackupCrypto.encrypt(plain, pw.toCharArray(), iterations = fastIterations)

            assertTrue(BackupCrypto.isEncrypted(enc1))
            assertEquals(plain, BackupCrypto.decrypt(enc1, pw.toCharArray()))
            assertEquals(plain, BackupCrypto.decrypt(enc2, pw.toCharArray()))

            val j1 = JSONObject(enc1)
            val j2 = JSONObject(enc2)
            assertNotEquals("Salt must be unique per encryption", j1.getString("salt"), j2.getString("salt"))
            assertNotEquals("IV must be unique per encryption", j1.getString("iv"), j2.getString("iv"))
            assertNotEquals("Ciphertext must be non-deterministic", j1.getString("data"), j2.getString("data"))
        }
    }

    @Test
    fun backupCrypto_anyTamperingOrWrongPassword_mustReject() {
        val pw = "Str0ng!Backup#2026"
        val envelope = BackupCrypto.encrypt("{\"secret\":12345}", pw.toCharArray(), iterations = fastIterations)

        try {
            BackupCrypto.decrypt(envelope, "Wrong!Backup#2026".toCharArray())
            fail("Wrong password must fail")
        } catch (_: WrongBackupPasswordException) {
        }

        val base = JSONObject(envelope)
        for (field in listOf("salt", "iv", "data")) {
            val mutated = JSONObject(envelope)
            val bytes = Base64.getDecoder().decode(base.getString(field))
            bytes[bytes.size / 2] = (bytes[bytes.size / 2].toInt() xor 0x01).toByte()
            mutated.put(field, Base64.getEncoder().encodeToString(bytes))
            try {
                BackupCrypto.decrypt(mutated.toString(), pw.toCharArray())
                fail("Mutating $field must fail authentication")
            } catch (_: WrongBackupPasswordException) {
            }
        }

        val aadTampered = JSONObject(envelope).put("iter", fastIterations + 1)
        try {
            BackupCrypto.decrypt(aadTampered.toString(), pw.toCharArray())
            fail("Mutating AAD-bound iter must fail")
        } catch (_: WrongBackupPasswordException) {
        }

        for (badIter in listOf(0, -1, 99_999, 5_000_001)) {
            val bad = JSONObject(envelope).put("iter", badIter)
            try {
                BackupCrypto.decrypt(bad.toString(), pw.toCharArray())
                fail("Out-of-bounds iter=$badIter must be rejected")
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    @Test
    fun localFileCipher_roundTripNonDeterminismAndEveryRegionTamperRejected() {
        val keyGen = KeyGenerator.getInstance("AES").apply { init(256) }
        val masterKey = keyGen.generateKey()
        val cipher = LocalFileCipher(masterKeyProvider = { masterKey })
        val rng = SecureRandom()

        val sizes = listOf(0, 1, 15, 16, 32, 255, 4096)
        for (size in sizes) {
            val plain = ByteArray(size).also(rng::nextBytes)
            val c1 = cipher.encrypt(plain)
            val c2 = cipher.encrypt(plain)

            assertTrue(LocalFileCipher.isEncrypted(c1))
            assertArrayEquals(plain, cipher.decrypt(c1))
            assertArrayEquals(plain, cipher.decrypt(c2))
            assertFalse("Two encryptions must not produce identical blobs", c1.contentEquals(c2))
        }

        val sample = cipher.encrypt("sensitive-pre-update-backup".toByteArray())
        val criticalOffsets = listOf(
            0,
            4,
            5,
            16,
            17,
            64,
            65,
            76,
            77,
            sample.lastIndex
        )
        for (offset in criticalOffsets) {
            val tampered = sample.copyOf()
            tampered[offset] = (tampered[offset].toInt() xor 0x80).toByte()
            try {
                cipher.decrypt(tampered)
                fail("Tampering byte at offset $offset must throw GeneralSecurityException")
            } catch (_: GeneralSecurityException) {
            }
        }

        for (len in listOf(0, 4, 5, 76, 92)) {
            val truncated = sample.copyOf(len)
            try {
                cipher.decrypt(truncated)
                fail("Truncated blob of len=$len must throw GeneralSecurityException")
            } catch (_: GeneralSecurityException) {
            }
        }
    }

    @Test
    fun databaseKeyManager_blobFormatExact60ByteBoundary() {
        val expectedBytes = DatabaseKeyManager.GCM_IV_LENGTH_BYTES +
            DatabaseKeyManager.PASSPHRASE_LENGTH_BYTES +
            DatabaseKeyManager.GCM_TAG_LENGTH_BYTES
        assertEquals(60, expectedBytes)

        val encoder = Base64.getEncoder()
        for (len in 0..120) {
            val blob = encoder.encodeToString(ByteArray(len) { 0x42 })
            assertEquals(
                "Length $len validity mismatch",
                len == expectedBytes,
                DatabaseKeyManager.isValidBlobFormat(blob)
            )
        }
        assertFalse(DatabaseKeyManager.isValidBlobFormat(""))
        assertFalse(DatabaseKeyManager.isValidBlobFormat("   "))
        assertFalse(DatabaseKeyManager.isValidBlobFormat("not-valid-base64!!!"))
    }

    @Test
    fun sessionLockPolicy_exhaustiveStateMatrix() {
        val elapsedSamples = listOf(
            Long.MIN_VALUE, -1000L, -1L, 0L, 1L,
            29_999L, 30_000L, 30_001L,
            60_000L, 60_001L,
            120_000L, 120_001L,
            300_000L, 300_001L,
            Long.MAX_VALUE
        )

        for (timeout in SessionLockPolicy.ALLOWED_TIMEOUTS_MS) {
            assertEquals(timeout, SessionLockPolicy.normalizeTimeout(timeout))
            for (bio in listOf(false, true)) {
                for (ext in listOf(false, true)) {
                    for (elapsed in elapsedSamples) {
                        val locked = SessionLockPolicy.shouldLock(elapsed, bio, timeout, ext)
                        if (!bio) {
                            assertFalse("Must not lock when biometrics disabled", locked)
                        } else if (elapsed < 0L) {
                            assertTrue("Negative clock skew must fail closed", locked)
                        } else if (ext) {
                            val grace = maxOf(timeout, SessionLockPolicy.EXTERNAL_FLOW_GRACE_MS)
                            assertEquals(elapsed > grace, locked)
                        } else {
                            assertEquals(timeout == 0L || elapsed > timeout, locked)
                        }
                    }
                }
            }
        }

        for (invalid in listOf(-1L, 1L, 15_000L, 120_000L, Long.MAX_VALUE)) {
            assertEquals(
                SessionLockPolicy.BACKGROUND_LOCK_TIMEOUT_MS,
                SessionLockPolicy.normalizeTimeout(invalid)
            )
        }
    }

    @Test
    fun safeHttpAndUpdatePolicies_adversarialUrlAndSignatureMatrix() {
        val disallowedSafeUrls = listOf(
            "http://api.github.com/repos/pauloribeiro16/finflow/releases/latest",
            "https://api.github.com:443/repos/pauloribeiro16/finflow/releases/latest",
            "https://api.github.com:8443/repos/pauloribeiro16/finflow/releases/latest",
            "https://user:pass@api.github.com/repos/pauloribeiro16/finflow/releases/latest",
            "https://api.github.com.evil.com/repos/pauloribeiro16/finflow/releases/latest",
            "https://evil.com/api.github.com",
            "file:///etc/passwd",
            ""
        )
        for (url in disallowedSafeUrls) {
            assertFalse("SafeHttp must reject $url", SafeHttp.isAllowedUrl(url))
        }
        assertTrue(SafeHttp.isAllowedUrl("https://api.github.com/repos/pauloribeiro16/finflow/releases/latest"))
        assertTrue(SafeHttp.isAllowedUrl("https://query1.finance.yahoo.com/v8/finance/chart/VWCE.DE"))

        val disallowedReleaseUrls = listOf(
            "http://github.com/pauloribeiro16/finflow/releases/download/v2.6.0/app.apk",
            "https://github.com:443/pauloribeiro16/finflow/releases/download/v2.6.0/app.apk",
            "https://github.com/pauloribeiro16/finflow/releases/download/../evil/app.apk",
            "https://github.com/otheruser/finflow/releases/download/v2.6.0/app.apk",
            "https://evil.com/pauloribeiro16/finflow/releases/download/v2.6.0/app.apk"
        )
        for (url in disallowedReleaseUrls) {
            assertFalse("UpdateUrlPolicy must reject release URL $url", UpdateUrlPolicy.isAllowedReleaseUrl(url))
        }
        assertTrue(UpdateUrlPolicy.isAllowedReleaseUrl("https://github.com/pauloribeiro16/finflow/releases/download/v2.7.0/Finflow.apk"))

        assertFalse(UpdateSignatureCheck.isSamePackageUpgrade("com.financas.app", 10L, "com.financas.app", 10L))
        assertFalse(UpdateSignatureCheck.isSamePackageUpgrade("com.financas.app", 10L, "com.financas.app", 9L))
        assertFalse(UpdateSignatureCheck.isSamePackageUpgrade("com.financas.app", 10L, "com.evil.app", 99L))
        assertTrue(UpdateSignatureCheck.isSamePackageUpgrade("com.financas.app", 10L, "com.financas.app", 11L))

        val official = UpdateSignatureCheck.RELEASE_CERT_SHA256
        assertFalse(UpdateSignatureCheck.isOfficialRelease(emptySet()))
        assertFalse(UpdateSignatureCheck.isOfficialRelease(setOf("deadbeef")))
        assertTrue(UpdateSignatureCheck.isOfficialRelease(official))
        assertFalse(UpdateSignatureCheck.isCompatible(official, emptySet()))
        assertFalse(UpdateSignatureCheck.isCompatible(emptySet(), official))
        assertFalse(UpdateSignatureCheck.isCompatible(official, setOf("deadbeef")))
        assertTrue(UpdateSignatureCheck.isCompatible(official, official))
    }

    @Test
    fun backupPasswordPolicy_rejectsWeakAndCommonPatterns() {
        val weakPasswords = listOf(
            "",
            "short",
            "1234567890",
            "aaaaaaaaaa",
            "abcdefghij",
            "password1234",
            "finflow2026!",
            "portugal123"
        )
        for (pw in weakPasswords) {
            assertFalse("Weak password '$pw' must be rejected", BackupPasswordPolicy.isAcceptable(pw))
        }
        assertTrue(BackupPasswordPolicy.isAcceptable("K9#mP2!vQ7\$zR4"))
    }

    @Test
    fun notificationSecurity_floodGuardBoundsAndParserFuzzSafety() {
        val now = 10_000_000L
        val fullQueue = List(PendingFloodGuard.MAX_PENDING_TOTAL) { idx ->
            "Bank${idx % 25}" to (now - 1000L)
        }
        assertFalse(PendingFloodGuard.shouldAccept(fullQueue, "NewBank", now))

        val sameSourceQueue = List(PendingFloodGuard.MAX_PER_SOURCE_PER_HOUR) {
            "MB WAY" to (now - 1000L)
        }
        assertFalse(PendingFloodGuard.shouldAccept(sameSourceQueue, "MB WAY", now))
        assertTrue(PendingFloodGuard.shouldAccept(sameSourceQueue, "Revolut", now))

        val parser = BankNotificationParser()
        val fuzzInputs = listOf(
            "",
            "€",
            "0,00 €",
            "-50,00 €",
            "Compra de 12,50 € no Continente",
            "'; DROP TABLE transactions; -- 10,00 €",
            "=HYPERLINK(\"http://evil\") 25,00 €"
        )
        for (text in fuzzInputs) {
            val res = parser.parse("pt.sibs.android.mbway", "MB WAY", text)
            if (res != null) {
                assertTrue("Parsed amountCents must be strictly positive", res.amountCents > 0L)
            }
        }
    }

    @Test
    fun notificationSecurity_rejectsSpoofedPackagesAndGuardsParserOverflow() {
        val spoofed = listOf(
            "com.evil.mbway",
            "org.attacker.revolut",
            "net.spam.wise",
            "com.fake.bcp",
            "com.evil.ordinary"
        )
        for (pkg in spoofed) {
            assertFalse("Spoofed package $pkg must be rejected", FinancialAppFilter.isFinancialApp(pkg))
        }
        assertTrue(FinancialAppFilter.isFinancialApp("pt.sibs.android.mbway"))
        assertTrue(FinancialAppFilter.isFinancialApp("com.revolut.revolut"))

        val parser = BankNotificationParser()
        assertEquals(null, parser.parse("pt.sibs.android.mbway", "MB WAY", "Pagou 999999999999999999 € em Loja"))
        val giantText = "Pagou 12,50 € em " + "A".repeat(50_000)
        val parsed = parser.parse("pt.sibs.android.mbway", "MB WAY", giantText)
        assertTrue("Truncated giant input must still parse cleanly", parsed != null && parsed.amountCents == 1250L)
    }

    @Test
    fun sessionBackgroundClock_externalFlowArmWindowExpiresAndIsSingleUse() {
        val clock = SessionBackgroundClock()
        clock.armExternalFlow(1_000L)
        clock.onActivityPaused(1_000L + SessionLockPolicy.EXTERNAL_FLOW_ARM_WINDOW_MS)
        assertTrue(clock.consumePausedForExternalFlow())
        assertFalse("External flow grace must be single-use", clock.consumePausedForExternalFlow())

        clock.armExternalFlow(10_000L)
        clock.onActivityPaused(10_000L + SessionLockPolicy.EXTERNAL_FLOW_ARM_WINDOW_MS + 1L)
        assertFalse("Expired external flow arm must not grant grace", clock.consumePausedForExternalFlow())
    }
}
