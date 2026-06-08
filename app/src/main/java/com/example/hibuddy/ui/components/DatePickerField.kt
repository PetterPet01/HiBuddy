package com.example.hibuddy.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val state = rememberDatePickerState()
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("DD/MM/YYYY") },
        modifier = modifier,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Choose $label")
            }
        }
    )
    if (open) {
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val format = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            onValueChange(format.format(Date(millis)))
                        }
                        open = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
