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
                e.titre, { e = e.copy(titre = it) }, label = { Text("Titre") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateChamp("Date", e.date, Modifier.weight(1.6f)) { e = e.copy(date = it) }
                HeureChamp("Heure", e.heureLocale(), Modifier.weight(1f), onJournee = { e = e.copy(heure = "") }) {
                    e = e.copy(heure = "%02d:%02d".format(it.hour, it.minute))
                }
            }
            Petit("Pour")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("thomas", "cindy", "deux").forEach { p ->
                    Choix(pourLabel(p), e.pour == p, pourColor(p)) { e = e.copy(pour = p) }
                }
            }
            Petit("Visible")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choix("Nous deux", !e.prive) { e = e.copy(prive = false) }
                Choix("Privé 🔒", e.prive) { e = e.copy(prive = true) }
            }
            Petit("Rappel")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RAPPELS.forEach { (m, l) -> Choix(l, e.rappel == m) { e = e.copy(rappel = m) } }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                e.note, { e = e.copy(note = it) }, label = { Text("Note") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isNew) OutlinedButton(onClick = { confirm = true }, modifier = Modifier.height(50.dp)) {
                    Text("Supprimer", color = Red)
                }
                Button(
                    onClick = { onSave(e) }, enabled = e.titre.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp),
                ) { Text("Enregistrer", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
            Spacer(Modifier.height(16.dp))
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

@Composable
private fun Petit(t: String) = Text(
    t, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
)
