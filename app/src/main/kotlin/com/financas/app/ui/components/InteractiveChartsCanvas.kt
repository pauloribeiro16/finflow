package com.financas.app.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.domain.model.MoneyCents
import com.financas.app.domain.usecase.CalendarDayData
import com.financas.app.domain.usecase.ChartScale
import com.financas.app.ui.i18n.LocalPrivacyMode
import com.financas.app.ui.theme.AccentAmber
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentCoral
import com.financas.app.ui.theme.AccentEmerald
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceHighlight
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

data class DonutSlice(
    val id: String,
    val name: String,
    val amountCents: Long,
    val percentage: Double,
    val color: Color
)

@Composable
fun InteractiveDonutCanvas(
    slices: List<DonutSlice>,
    selectedSliceId: String?,
    onSelectSlice: (String?) -> Unit,
    centerTitle: String,
    centerValue: String,
    centerSubtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val totalCents = slices.sumOf { it.amountCents }
    val emptyDonutColor = DarkSurfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slices, selectedSliceId) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = tapOffset.x - centerX
                        val dy = tapOffset.y - centerY
                        val dist = sqrt(dx * dx + dy * dy)

                        val radius = min(size.width, size.height) / 2f * 0.82f
                        val strokeWidth = radius * 0.28f
                        val innerRadius = radius - strokeWidth

                        if (dist < innerRadius * 0.65f) {
                            onSelectSlice(null)
                        } else if (dist in (innerRadius * 0.65f)..(radius * 1.35f)) {
                            if (totalCents > 0L && slices.isNotEmpty()) {
                                val rawAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                val normalizedAngle = (rawAngle + 90f + 360f) % 360f

                                var cumAngle = 0f
                                var clickedSliceId: String? = null
                                for (slice in slices) {
                                    val sweep = (slice.amountCents.toFloat() / totalCents.toFloat()) * 360f
                                    if (normalizedAngle >= cumAngle && normalizedAngle < cumAngle + sweep) {
                                        clickedSliceId = slice.id
                                        break
                                    }
                                    cumAngle += sweep
                                }

                                if (clickedSliceId != null) {
                                    if (clickedSliceId == selectedSliceId) {
                                        onSelectSlice(null)
                                    } else {
                                        onSelectSlice(clickedSliceId)
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val canvasRadius = min(size.width, size.height) / 2f * 0.82f
            val baseStrokeWidth = canvasRadius * 0.28f
            val centerOffset = Offset(size.width / 2f, size.height / 2f)

            if (totalCents <= 0L || slices.isEmpty()) {
                drawCircle(
                    color = emptyDonutColor,
                    radius = canvasRadius,
                    center = centerOffset,
                    style = Stroke(width = baseStrokeWidth)
                )
            } else {
                var currentAngle = -90f
                val hasSelection = selectedSliceId != null
                val gap = if (slices.size > 1) 2f else 0f

                slices.forEach { slice ->
                    val sweep = (slice.amountCents.toFloat() / totalCents.toFloat()) * 360f
                    val sweepWithGap = (sweep - gap).coerceAtLeast(0.5f)
                    val isSelected = slice.id == selectedSliceId

                    val stroke = if (isSelected) baseStrokeWidth * 1.35f else baseStrokeWidth
                    val sliceRadius = if (isSelected) canvasRadius + 4.dp.toPx() else canvasRadius
                    val sliceColor = if (hasSelection && !isSelected) {
                        slice.color.copy(alpha = 0.40f)
                    } else {
                        slice.color
                    }

                    drawArc(
                        color = sliceColor,
                        startAngle = currentAngle + (gap / 2f),
                        sweepAngle = sweepWithGap,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - sliceRadius, centerOffset.y - sliceRadius),
                        size = Size(sliceRadius * 2f, sliceRadius * 2f),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    currentAngle += sweep
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = centerTitle,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = centerValue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            if (!centerSubtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = centerSubtitle,
                    fontSize = 11.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

data class ParetoBarItem(
    val id: String,
    val label: String,
    val amountCents: Long,
    val formattedValue: String,
    val percentage: Double,
    val color: Color,
    val secondaryText: String? = null
)

@Composable
fun HorizontalParetoBars(
    items: List<ParetoBarItem>,
    modifier: Modifier = Modifier,
    maxAmountCents: Long = items.maxOfOrNull { it.amountCents } ?: 0L,
    onItemClick: ((String) -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onItemClick != null) {
                            Modifier.clickable { onItemClick(item.id) }
                        } else Modifier
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.formattedValue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurfaceVariant)
                ) {
                    val maxVal = if (maxAmountCents > 0L) maxAmountCents.toFloat() else 1f
                    val ratio = (item.amountCents.toFloat() / maxVal).coerceIn(0f, 1f)
                    val fillWidth = size.width * ratio

                    if (fillWidth > 0f) {
                        drawRoundRect(
                            color = item.color,
                            topLeft = Offset(0f, 0f),
                            size = Size(fillWidth, size.height),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                }

                if (!item.secondaryText.isNullOrBlank()) {
                    Text(
                        text = item.secondaryText,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawYAxisLabels(
    ticks: List<Double>,
    scaleMax: Double,
    topPadding: Float,
    chartHeight: Float,
    labelRightX: Float,
    labelColor: Color,
    format: (Double) -> String
) {
    if (ticks.isEmpty()) return
    val paint = Paint().apply {
        color = labelColor.toArgb()
        textSize = 10.sp.toPx()
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }
    val baselineOffset = paint.textSize / 3f
    ticks.forEach { tick ->
        val ratio = (tick / scaleMax).coerceIn(0.0, 1.0)
        val y = topPadding + chartHeight * (1f - ratio.toFloat())
        drawContext.canvas.nativeCanvas.drawText(
            format(tick),
            labelRightX,
            y + baselineOffset,
            paint
        )
    }
}

data class MonthlyBarData(
    val yearMonth: Int,
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val netSavingsCents: Long,
    val savingsRatePercent: Double
)

@Composable
fun InteractiveMonthlyBarChart(
    months: List<MonthlyBarData>,
    selectedYearMonth: Int?,
    onSelectYearMonth: (Int) -> Unit,
    monthlyAverageExpenseCents: Long,
    modifier: Modifier = Modifier
) {
    if (months.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AccentEmerald)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Receita", fontSize = 11.sp, color = TextSecondary)

            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AccentCoral)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Despesa", fontSize = 11.sp, color = TextSecondary)

            if (monthlyAverageExpenseCents > 0L) {
                val avgLegendLineColor = TextSecondary
                Spacer(modifier = Modifier.width(12.dp))
                Canvas(modifier = Modifier.size(width = 16.dp, height = 2.dp)) {
                    drawLine(
                        color = avgLegendLineColor,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Média Despesa", fontSize = 11.sp, color = TextSecondary)
            }
        }

        // Chart Canvas
        val isPrivacy = LocalPrivacyMode.current
        val highlightPillColor = DarkSurfaceHighlight
        val avgDashedLineColor = TextSecondary.copy(alpha = 0.6f)
        val incomeBarColor = AccentEmerald
        val expenseBarColor = AccentCoral
        val gridLineColor = DarkBorder
        val axisLabelColor = TextMuted
        val axisGutter = 40.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(months) {
                        detectTapGestures { tapOffset ->
                            val gutterPx = axisGutter.toPx()
                            val slotWidth = (size.width - gutterPx) / months.size.toFloat()
                            val index = ((tapOffset.x - gutterPx) / slotWidth).toInt().coerceIn(0, months.size - 1)
                            onSelectYearMonth(months[index].yearMonth)
                        }
                    }
                    .pointerInput(months) {
                        detectDragGestures { change, _ ->
                            val gutterPx = axisGutter.toPx()
                            val slotWidth = (size.width - gutterPx) / months.size.toFloat()
                            val index = ((change.position.x - gutterPx) / slotWidth).toInt().coerceIn(0, months.size - 1)
                            onSelectYearMonth(months[index].yearMonth)
                        }
                    }
            ) {
                val maxCents = maxOf(
                    months.maxOfOrNull { maxOf(it.incomeCents, it.expenseCents) } ?: 0L,
                    monthlyAverageExpenseCents,
                    1L
                )
                val ticks = ChartScale.computeTicks(maxCents.toDouble(), 3)
                val scaleMax = (ticks.lastOrNull() ?: maxCents.toDouble()).coerceAtLeast(1.0)
                val scaleMaxF = scaleMax.toFloat()

                val gutterPx = axisGutter.toPx()
                val bottomPadding = 4.dp.toPx()
                val topPadding = 12.dp.toPx()
                val chartHeight = size.height - bottomPadding - topPadding
                val slotWidth = (size.width - gutterPx) / months.size.toFloat()
                val barWidth = (slotWidth * 0.32f).coerceAtLeast(3f)
                val barGap = 2.dp.toPx()

                // Y axis grid lines and compact labels
                ticks.forEach { tick ->
                    val ratio = (tick / scaleMax).coerceIn(0.0, 1.0)
                    val gridY = topPadding + chartHeight * (1f - ratio.toFloat())
                    drawLine(
                        color = gridLineColor,
                        start = Offset(gutterPx, gridY),
                        end = Offset(size.width, gridY),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                drawYAxisLabels(
                    ticks = ticks,
                    scaleMax = scaleMax,
                    topPadding = topPadding,
                    chartHeight = chartHeight,
                    labelRightX = gutterPx - 6.dp.toPx(),
                    labelColor = axisLabelColor,
                    format = { ChartScale.formatCompactCents(it.roundToLong(), isPrivacy) }
                )

                // Highlight background pill for selected month
                val selectedIdx = months.indexOfFirst { it.yearMonth == selectedYearMonth }
                if (selectedIdx >= 0) {
                    val pillLeft = gutterPx + selectedIdx * slotWidth + 2.dp.toPx()
                    val pillWidth = slotWidth - 4.dp.toPx()
                    drawRoundRect(
                        color = highlightPillColor,
                        topLeft = Offset(pillLeft, 0f),
                        size = Size(pillWidth, size.height),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }

                // Average Expense dashed line
                if (monthlyAverageExpenseCents > 0L) {
                    val avgRatio = (monthlyAverageExpenseCents.toFloat() / scaleMaxF).coerceIn(0f, 1f)
                    val avgY = topPadding + chartHeight * (1f - avgRatio)

                    drawLine(
                        color = avgDashedLineColor,
                        start = Offset(gutterPx, avgY),
                        end = Offset(size.width, avgY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                }

                // Dual bars for each month
                months.forEachIndexed { i, month ->
                    val slotCenterX = gutterPx + (i * slotWidth) + (slotWidth / 2f)

                    val incRatio = (month.incomeCents.toFloat() / scaleMaxF).coerceIn(0f, 1f)
                    val incHeight = chartHeight * incRatio
                    val incTop = topPadding + chartHeight - incHeight
                    val incLeft = slotCenterX - barWidth - (barGap / 2f)

                    if (incHeight > 0f) {
                        drawRoundRect(
                            color = incomeBarColor,
                            topLeft = Offset(incLeft, incTop),
                            size = Size(barWidth, incHeight),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                    }

                    val expRatio = (month.expenseCents.toFloat() / scaleMaxF).coerceIn(0f, 1f)
                    val expHeight = chartHeight * expRatio
                    val expTop = topPadding + chartHeight - expHeight
                    val expLeft = slotCenterX + (barGap / 2f)

                    if (expHeight > 0f) {
                        drawRoundRect(
                            color = expenseBarColor,
                            topLeft = Offset(expLeft, expTop),
                            size = Size(barWidth, expHeight),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                    }
                }
            }
        }

        // Labels row below bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            months.forEach { month ->
                val isSelected = month.yearMonth == selectedYearMonth
                Text(
                    text = month.label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) AccentBlue else TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

data class LinePoint(
    val label: String,
    val value: Double,
    val formattedValue: String
)

@Composable
fun InteractiveLineChart(
    points: List<LinePoint>,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit,
    averageValue: Double?,
    averageLabel: String?,
    referenceValue: Double? = null,
    referenceLabel: String? = null,
    tickFormatter: ((Double) -> String)? = null,
    targetTickCount: Int = 3,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val showAverageLegend = averageValue != null && averageLabel != null
        val showReferenceLegend = referenceValue != null && referenceLabel != null
        if (showAverageLegend || showReferenceLegend) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showAverageLegend) {
                    val avgLegendLineColor = TextSecondary
                    Canvas(modifier = Modifier.size(width = 16.dp, height = 2.dp)) {
                        drawLine(
                            color = avgLegendLineColor,
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(averageLabel, fontSize = 11.sp, color = TextSecondary)
                }
                if (showReferenceLegend) {
                    val refLegendLineColor = AccentAmber
                    Spacer(modifier = Modifier.width(12.dp))
                    Canvas(modifier = Modifier.size(width = 16.dp, height = 2.dp)) {
                        drawLine(
                            color = refLegendLineColor,
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(referenceLabel, fontSize = 11.sp, color = AccentAmber)
                }
            }
        }

        val avgLineColor = TextSecondary.copy(alpha = 0.6f)
        val refLineColor = AccentAmber.copy(alpha = 0.8f)
        val dotCenterColor = TextPrimary
        val gridLineColor = DarkBorder
        val axisLabelColor = TextMuted
        val axisGutter = if (tickFormatter != null) 40.dp else 0.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points, tickFormatter) {
                        detectTapGestures { tapOffset ->
                            val gutterPx = axisGutter.toPx()
                            val plotLeft = gutterPx + 6.dp.toPx()
                            val plotWidth = size.width - gutterPx - 12.dp.toPx()
                            onSelectIndex(nearestLineIndex(tapOffset.x, plotLeft, plotWidth, points.size))
                        }
                    }
                    .pointerInput(points, tickFormatter) {
                        detectDragGestures { change, _ ->
                            val gutterPx = axisGutter.toPx()
                            val plotLeft = gutterPx + 6.dp.toPx()
                            val plotWidth = size.width - gutterPx - 12.dp.toPx()
                            onSelectIndex(nearestLineIndex(change.position.x, plotLeft, plotWidth, points.size))
                        }
                    }
            ) {
                val maxValue = maxOf(
                    points.maxOfOrNull { it.value } ?: 0.0,
                    averageValue ?: 0.0,
                    referenceValue ?: 0.0
                )
                val maxSafe = if (maxValue > 0.0) maxValue else 1.0
                val ticks = tickFormatter
                    ?.let { ChartScale.computeTicks(maxSafe, targetTickCount) }
                    ?: emptyList()
                val scaleMax = ticks.lastOrNull() ?: maxSafe

                val gutterPx = axisGutter.toPx()
                val topPadding = 12.dp.toPx()
                val bottomPadding = 12.dp.toPx()
                val chartHeight = size.height - topPadding - bottomPadding
                val horizontalPadding = 6.dp.toPx()
                val plotLeft = gutterPx + horizontalPadding
                val spanWidth = size.width - gutterPx - horizontalPadding * 2f

                fun yFor(value: Double): Float {
                    val ratio = (value / scaleMax).coerceIn(0.0, 1.0)
                    return (topPadding + chartHeight * (1f - ratio.toFloat()))
                }

                fun xFor(index: Int): Float {
                    if (points.size <= 1) return plotLeft + spanWidth / 2f
                    val step = spanWidth / (points.size - 1).toFloat()
                    return plotLeft + index * step
                }

                if (ticks.isNotEmpty()) {
                    ticks.forEach { tick ->
                        val gridY = yFor(tick)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(gutterPx, gridY),
                            end = Offset(size.width, gridY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    tickFormatter?.let { formatter ->
                        drawYAxisLabels(
                            ticks = ticks,
                            scaleMax = scaleMax,
                            topPadding = topPadding,
                            chartHeight = chartHeight,
                            labelRightX = gutterPx - 6.dp.toPx(),
                            labelColor = axisLabelColor,
                            format = formatter
                        )
                    }
                }

                if (averageValue != null && averageValue > 0.0) {
                    val avgY = yFor(averageValue)
                    drawLine(
                        color = avgLineColor,
                        start = Offset(gutterPx, avgY),
                        end = Offset(size.width, avgY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                }

                if (referenceValue != null && referenceValue > 0.0) {
                    val refY = yFor(referenceValue)
                    drawLine(
                        color = refLineColor,
                        start = Offset(gutterPx, refY),
                        end = Offset(size.width, refY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                }

                val linePath = Path()
                points.forEachIndexed { index, point ->
                    val x = xFor(index)
                    val y = yFor(point.value)
                    if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }
                drawPath(
                    color = lineColor,
                    path = linePath,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { index, point ->
                    val center = Offset(xFor(index), yFor(point.value))
                    val isSelected = index == selectedIndex
                    if (isSelected) {
                        drawCircle(
                            color = lineColor.copy(alpha = 0.3f),
                            radius = 8.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 5.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = dotCenterColor,
                            radius = 2.5.dp.toPx(),
                            center = center
                        )
                    } else if (point.value > 0.0) {
                        drawCircle(
                            color = lineColor,
                            radius = 3.dp.toPx(),
                            center = center
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                Text(
                    text = point.label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) AccentBlue else TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun nearestLineIndex(positionX: Float, plotLeft: Float, plotWidth: Float, pointCount: Int): Int {
    if (pointCount <= 1) return 0
    val step = plotWidth / (pointCount - 1).toFloat()
    return ((positionX - plotLeft) / step).roundToInt().coerceIn(0, pointCount - 1)
}

data class YearlyBarData(
    val year: Int,
    val monthlyExpenseCents: List<Long>,
    val totalExpenseCents: Long,
    val color: Color
)

@Composable
fun InteractiveYearComparisonChart(
    years: List<YearlyBarData>,
    modifier: Modifier = Modifier
) {
    if (years.isEmpty()) return

    val isPrivacy = LocalPrivacyMode.current
    val monthLabels = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            years.forEachIndexed { index, yearData ->
                if (index > 0) Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(yearData.color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val isPrivacy = LocalPrivacyMode.current
                Text(
                    text = "${yearData.year} · ${if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(yearData.totalExpenseCents, false)}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val gridLineColor = DarkBorder
            val axisLabelColor = TextMuted
            Canvas(modifier = Modifier.fillMaxSize()) {
                val globalMax = years.maxOfOrNull { yearData ->
                    yearData.monthlyExpenseCents.maxOfOrNull { it } ?: 0L
                } ?: 0L
                val maxCents = maxOf(globalMax, 1L)
                val ticks = ChartScale.computeTicks(maxCents.toDouble(), 3)
                val scaleMax = (ticks.lastOrNull() ?: maxCents.toDouble()).coerceAtLeast(1.0)
                val scaleMaxF = scaleMax.toFloat()

                val gutterPx = 40.dp.toPx()
                val topPadding = 12.dp.toPx()
                val bottomPadding = 4.dp.toPx()
                val chartHeight = size.height - topPadding - bottomPadding
                val slotWidth = (size.width - gutterPx) / 12f
                val barWidth = (slotWidth / (years.size + 1).toFloat()).coerceAtLeast(2f)
                val barGap = if (years.size > 1) 1.dp.toPx() else 0f
                val groupWidth = barWidth * years.size + barGap * (years.size - 1)

                ticks.forEach { tick ->
                    val ratio = (tick / scaleMax).coerceIn(0.0, 1.0)
                    val gridY = topPadding + chartHeight * (1f - ratio.toFloat())
                    drawLine(
                        color = gridLineColor,
                        start = Offset(gutterPx, gridY),
                        end = Offset(size.width, gridY),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                drawYAxisLabels(
                    ticks = ticks,
                    scaleMax = scaleMax,
                    topPadding = topPadding,
                    chartHeight = chartHeight,
                    labelRightX = gutterPx - 6.dp.toPx(),
                    labelColor = axisLabelColor,
                    format = { ChartScale.formatCompactCents(it.roundToLong(), isPrivacy) }
                )

                years.forEachIndexed { seriesIndex, yearData ->
                    for (monthIdx in 0..11) {
                        val cents = yearData.monthlyExpenseCents.getOrNull(monthIdx) ?: 0L
                        if (cents <= 0L) continue
                        val ratio = (cents.toFloat() / scaleMaxF).coerceIn(0f, 1f)
                        val barHeight = chartHeight * ratio
                        val slotLeft = gutterPx + monthIdx * slotWidth
                        val barLeft = slotLeft + (slotWidth - groupWidth) / 2f + seriesIndex * (barWidth + barGap)

                        drawRoundRect(
                            color = yearData.color,
                            topLeft = Offset(barLeft, topPadding + chartHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            monthLabels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private val CALENDAR_WEEKDAY_LABELS = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

@Composable
fun MonthlyCalendarGrid(
    days: List<CalendarDayData>,
    selectedEpochDay: Long?,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return

    val maxSpendCents = days.maxOfOrNull { it.expenseCents } ?: 0L
    val leadingEmptyCells = days.firstOrNull()
        ?.let { LocalDate.ofEpochDay(it.epochDay).dayOfWeek.value - 1 }
        ?: 0

    val cells = buildList<CalendarDayData?> {
        repeat(leadingEmptyCells) { add(null) }
        days.forEach { add(it) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CALENDAR_WEEKDAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (position in 0 until 7) {
                    val day = week.getOrNull(position)
                    if (day == null) {
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        CalendarDayCell(
                            day = day,
                            maxSpendCents = maxSpendCents,
                            isSelected = day.epochDay == selectedEpochDay,
                            onClick = { onSelectDay(day.epochDay) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDayData,
    maxSpendCents: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val intensity = if (maxSpendCents <= 0L || day.expenseCents <= 0L) {
        0f
    } else {
        (day.expenseCents.toFloat() / maxSpendCents.toFloat()).coerceIn(0f, 1f) * 0.55f
    }

    val borderColor = when {
        isSelected -> AccentBlue
        day.isToday -> AccentBlue.copy(alpha = 0.7f)
        else -> DarkBorder
    }
    val borderWidth = if (isSelected || day.isToday) 1.5.dp else 1.dp

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .background(AccentCoral.copy(alpha = intensity))
            .background(if (isSelected) AccentBlue.copy(alpha = 0.30f) else Color.Transparent)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            fontSize = 12.sp,
            fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.expenseCents > 0L || isSelected || day.isToday) TextPrimary else TextMuted
        )
    }
}
