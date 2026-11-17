package com.koala.koalagenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
private fun Champ(label: String, valeur: String, modifier: Modifier, onClick: () -> Unit) = Column(
    modifier.clip(RoundedCornerShape(12.dp)).background(Navy3).clickable { onClick() }
        .padding(horizontal = 14.dp, vertical = 10.dp),
) {
    Text(label, color = Muted, fontSize = 12.sp)
    Text(valeur, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateChamp(label: String, date: LocalDate, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Champ(label, fmtJour(date) + " " + date.year, modifier) { open = true }
    if (open) {
        val st = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Annuler") } },
        ) { DatePicker(st, showModeToggle = false) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeureChamp(label: String, heure: LocalTime, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Champ(label, "%02dh%02d".format(heure.hour, heure.minute), modifier) { open = true }
    if (open) {
        val st = rememberTimePickerState(heure.hour, heure.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = { onChange(LocalTime.of(st.hour, st.minute)); open = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Annuler") } },
            text = { TimePicker(st) },
        )
    }
}
