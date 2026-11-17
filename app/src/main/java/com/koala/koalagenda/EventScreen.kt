package com.koala.koalagenda

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventScreen(
    initial: Evenement,
    isNew: Boolean,
    moi: Who,
    onSave: (Evenement) -> Unit,
    onDelete: (Evenement) -> Unit,
    onBack: () -> Unit,
) {
    var e by remember(initial.id) { mutableStateOf(initial) }
    var confirm by remember { mutableStateOf(false) }
    BackHandler { onBack() }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        TopBar(if (isNew) "Nouveau rdv" else "Rdv", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                e.titre, { e = e.copy(titre = it) }, label = { Text("Quoi ? (dentiste, anniversaire…)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            DateChamp("Date", e.date) { e = e.copy(date = it) }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Toute la journée", color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Switch(checked = e.journee, onCheckedChange = { j -> e = e.copy(heure = if (j) "" else "14:00") })
            }
            if (!e.journee) {
                Spacer(Modifier.height(8.dp))
                HeureChamp("Heure", e.heureLocale() ?: LocalTime.of(14, 0)) {
                    e = e.copy(heure = "%02d:%02d".format(it.hour, it.minute))
                }
            }

            Section("Pour qui")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("thomas", "cindy", "deux").forEach { p ->
                    Choix(pourLabel(p), e.pour == p, pourColor(p)) { e = e.copy(pour = p) }
                }
            }

            Section("Visible par")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Choix("Nous deux", !e.prive) { e = e.copy(prive = false) }
                Choix("Moi seulement (privé)", e.prive) { e = e.copy(prive = true) }
            }
            if (e.prive) Text(
                "Masqué sur le téléphone de ${if (moi == Who.THOMAS) "Cindy" else "Thomas"}",
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
            )

            Section("Rappel")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RAPPELS.forEach { (m, l) -> Choix(l, e.rappel == m) { e = e.copy(rappel = m) } }
            }
            if (e.journee && e.rappel >= 0) Text(
                "Journée entière : rappel calculé sur 9h", color = Muted, fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                e.note, { e = e.copy(note = it) }, label = { Text("Note (lieu, détails…)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onSave(e) }, enabled = e.titre.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text("Enregistrer", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            if (!isNew) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Supprimer", color = Red)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Supprimer ce rdv ?") },
        text = { Text(e.titre) },
        confirmButton = { TextButton(onClick = { confirm = false; onDelete(initial) }) { Text("Supprimer", color = Red) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Annuler") } },
    )
}
