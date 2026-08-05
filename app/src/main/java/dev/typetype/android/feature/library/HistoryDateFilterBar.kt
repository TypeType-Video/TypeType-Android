package dev.typetype.android.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryDateFilterBar(
    selection: HistoryDateSelection,
    selectedDateMillis: Long?,
    canClearHistory: Boolean,
    onSelectionChange: (HistoryDateSelection, Long?) -> Unit,
    onClearHistory: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val options = listOf(
        HistoryDateSelection.Today to stringResource(R.string.library_history_today),
        HistoryDateSelection.ThisWeek to stringResource(R.string.library_history_this_week),
        HistoryDateSelection.ThisMonth to stringResource(R.string.library_history_this_month),
    )
    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(options, key = { it.first.name }) { (option, label) ->
                FilterChip(
                    selected = selection == option,
                    onClick = {
                        onSelectionChange(
                            if (selection == option) HistoryDateSelection.All else option,
                            null,
                        )
                    },
                    label = { Text(label) },
                )
            }
            item(key = "specific-date") {
                FilterChip(
                    selected = selection == HistoryDateSelection.SpecificDay,
                    onClick = { showDatePicker = true },
                    leadingIcon = {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    },
                    label = {
                        Text(
                            selectedDateMillis?.takeIf {
                                selection == HistoryDateSelection.SpecificDay
                            }?.let(::formatHistoryDate)
                                ?: stringResource(R.string.library_history_choose_date),
                        )
                    },
                )
            }
        }
        if (canClearHistory) {
            TextButton(
                onClick = { showClearConfirmation = true },
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                Text(stringResource(R.string.library_history_clear_all))
            }
        }
    }
    if (showDatePicker) {
        val today = remember { LocalDate.now() }
        val selectableDates = remember(today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= today.toPickerMillis()

                override fun isSelectableYear(year: Int): Boolean = year <= today.year
            }
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis,
            selectableDates = selectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        val selected = pickerState.selectedDateMillis ?: return@TextButton
                        showDatePicker = false
                        onSelectionChange(HistoryDateSelection.SpecificDay, selected)
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.settings_privacy_confirm_clear_history)) },
            text = { Text(stringResource(R.string.library_history_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirmation = false
                    onClearHistory()
                }) {
                    Text(stringResource(R.string.library_history_clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private fun formatHistoryDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
