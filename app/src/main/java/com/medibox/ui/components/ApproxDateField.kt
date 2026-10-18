package com.medibox.ui.components

import android.app.DatePickerDialog
import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.medibox.domain.ApproxDate
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.BorderStrong
import com.medibox.ui.theme.Ink
import com.medibox.ui.theme.InkSecondary
import com.medibox.ui.theme.InkTertiary
import com.medibox.ui.theme.RadiusMd
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.Space3
import com.medibox.ui.theme.Space4
import com.medibox.ui.theme.SurfaceSunken
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

@Composable
fun ApproxDateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "选择日期",
) {
    var pickerMode by remember { mutableStateOf<DatePickerMode?>(null) }
    val parsed = remember(value) { value.toApproxDateOrNull() }
    val displayText = parsed?.toHumanText() ?: value.ifBlank { placeholder }
    val isEmpty = value.isBlank()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space2)) {
        if (label.isNotBlank()) {
            Text(text = label, style = AppText.Label, color = Ink)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceSunken, RadiusMd)
                .clickable { pickerMode = DatePickerMode.Choice }
                .padding(horizontal = Space4, vertical = Space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space3)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space2)) {
                Text(
                    text = displayText,
                    style = AppText.Body,
                    color = if (isEmpty) InkTertiary else Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "点击选择日期精度",
                    style = AppText.Caption,
                    color = InkSecondary,
                )
            }
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = InkSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        if (!value.isBlank()) {
            TextButton(onClick = { onValueChange("") }) {
                Text("清除日期")
            }
        }
    }

    when (pickerMode) {
        DatePickerMode.Choice -> {
            DatePrecisionChooserDialog(
                hasValue = value.isNotBlank(),
                onChooseYearMonth = { pickerMode = DatePickerMode.YearMonth },
                onChooseFullDate = { pickerMode = DatePickerMode.FullDate },
                onClear = {
                    onValueChange("")
                    pickerMode = null
                },
                onDismiss = { pickerMode = null }
            )
        }
        DatePickerMode.YearMonth -> {
            MonthYearPickerDialog(
                initial = parsed,
                onDismiss = { pickerMode = null },
                onConfirm = { date ->
                    onValueChange(date.toStorageString())
                    pickerMode = null
                }
            )
        }
        DatePickerMode.FullDate -> {
            FullDatePickerDialog(
                initial = parsed,
                onDismiss = { pickerMode = null },
                onConfirm = { date ->
                    onValueChange(date.toStorageString())
                    pickerMode = null
                }
            )
        }
        null -> Unit
    }
}

@Composable
private fun DatePrecisionChooserDialog(
    hasValue: Boolean,
    onChooseYearMonth: () -> Unit,
    onChooseFullDate: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期精度") },
        text = { Text("先选择“年月”或“完整日期”。保存时会保留你选择的精度。") },
        confirmButton = {
            TextButton(onClick = onChooseYearMonth) { Text("年月") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Space2)) {
                if (hasValue) {
                    TextButton(onClick = onClear) { Text("清除") }
                }
                TextButton(onClick = onChooseFullDate) { Text("完整日期") }
            }
        }
    )
}

@Composable
private fun MonthYearPickerDialog(
    initial: ApproxDate?,
    onDismiss: () -> Unit,
    onConfirm: (ApproxDate) -> Unit,
) {
    val currentYear = LocalDate.now().year
    val currentMonth = LocalDate.now().monthValue
    var year by remember(initial) { mutableIntStateOf(initial?.year ?: currentYear) }
    var month by remember(initial) { mutableIntStateOf(initial?.month ?: currentMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择年月") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space4)) {
                Text("保存为“年月”精度。", style = AppText.Secondary, color = InkSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(Space3), modifier = Modifier.fillMaxWidth()) {
                    PickerColumn(
                        label = "年",
                        value = year,
                        range = (currentYear - 8)..(currentYear + 12),
                        onValueChange = { year = it },
                        modifier = Modifier.weight(1f)
                    )
                    PickerColumn(
                        label = "月",
                        value = month,
                        range = 1..12,
                        onValueChange = { month = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(ApproxDate(ApproxDate.Precision.YEAR_MONTH, year, month)) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PickerColumn(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space2)
    ) {
        Text(label, style = AppText.Caption, color = InkSecondary)
        AndroidView(
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = range.first
                    maxValue = range.last
                    wrapSelectorWheel = false
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    setOnValueChangedListener { _, _, newVal -> onValueChange(newVal) }
                }
            },
            update = { picker ->
                picker.minValue = range.first
                picker.maxValue = range.last
                if (picker.value != value) {
                    picker.value = value.coerceIn(range)
                }
            }
        )
    }
}

@Composable
private fun FullDatePickerDialog(
    initial: ApproxDate?,
    onDismiss: () -> Unit,
    onConfirm: (ApproxDate) -> Unit,
) {
    val context = LocalContext.current
    val initialDate = remember(initial) {
        initial?.toLocalDate()
            ?: LocalDate.now()
    }
    DisposableEffect(initialDate) {
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onConfirm(ApproxDate(ApproxDate.Precision.FULL_DATE, year, month + 1, dayOfMonth))
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        )
        dialog.setOnCancelListener { onDismiss() }
        dialog.show()
        onDispose {
            dialog.dismiss()
        }
    }
}

private sealed interface DatePickerMode {
    data object Choice : DatePickerMode
    data object YearMonth : DatePickerMode
    data object FullDate : DatePickerMode
}

fun ApproxDate.toHumanText(): String = when (precision) {
    ApproxDate.Precision.YEAR_MONTH -> "${year}年${requireNotNull(month)}月"
    ApproxDate.Precision.FULL_DATE -> "${year}年${requireNotNull(month)}月${requireNotNull(day)}日"
}

fun ApproxDate?.toHumanTextOrBlank(): String = this?.toHumanText().orEmpty()

fun String.toReadableApproxDateOrSelf(): String {
    return runCatching { ApproxDate.parse(this).toHumanText() }.getOrElse { this }
}

fun String.toApproxDateOrNull(): ApproxDate? {
    if (isBlank()) return null
    return runCatching { ApproxDate.parse(this) }.getOrNull()
}

private fun ApproxDate.toLocalDate(): LocalDate = when (precision) {
    ApproxDate.Precision.YEAR_MONTH -> LocalDate.of(year, requireNotNull(month), 1)
    ApproxDate.Precision.FULL_DATE -> LocalDate.of(year, requireNotNull(month), requireNotNull(day))
}
