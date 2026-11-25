package com.koala.koalagenda

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

val CouleurAnniv = Color(0xFFFFB3DE)

/** Ligne d'un anniversaire (liste et vue du jour). */
@Composable
fun LigneAnniversaire(a: Anniversaire, date: LocalDate, montrerDate: Boolean, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(Navy2)
        .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text("🎂", fontSize = 20.sp)
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
        Text(a.nom, color = Encre, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val age = a.age(date.year)
        val detail = listOfNotNull(
            if (montrerDate) "${date.dayOfMonth} ${MOIS[date.monthValue - 1]}" else null,
            age?.let { "$it ans" },
        ).joinToString(" · ")
        if (detail.isNotEmpty()) Text(detail, color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun AnniversairesScreen(state: AgendaState, onEdit: (Anniversaire) -> Unit, onNew: () -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    val today = LocalDate.now()
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        TopBar("Anniversaires", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            state.anniversaires.sortedBy { it.prochain(today) }.forEach { a ->
                LigneAnniversaire(a, a.prochain(today), montrerDate = true) { onEdit(a) }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onNew, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("+ Anniversaire", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnniversaireEditScreen(
    initial: Anniversaire,
    isNew: Boolean,
    onSave: (Anniversaire) -> Unit,
    onDelete: (Anniversaire) -> Unit,
    onBack: () -> Unit,
) {
    var nom by remember { mutableStateOf(initial.nom) }
    var jour by remember { mutableStateOf(if (isNew) "" else initial.jour.toString()) }
    var mois by remember { mutableStateOf(initial.mois) }
    var annee by remember { mutableStateOf(if (initial.annee > 0) initial.annee.toString() else "") }
    var confirm by remember { mutableStateOf(false) }
    BackHandler { onBack() }

    val j = jour.toIntOrNull() ?: 0
    val an = annee.toIntOrNull() ?: 0
    val ok = nom.isNotBlank() && Anniversaire.valide(j, mois) && (annee.isBlank() || an in 1900..LocalDate.now().year)

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        TopBar(if (isNew) "Nouvel anniversaire" else "Anniversaire", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            OutlinedTextField(nom, { nom = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    jour, { jour = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Jour") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    annee, { annee = it.filter { c -> c.isDigit() }.take(4) }, label = { Text("Année (facultatif)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1.6f),
                )
            }
            Text("Mois", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MOIS.forEachIndexed { i, m ->
                    Choix(m.take(4).replaceFirstChar { it.uppercase() }, mois == i + 1, CouleurAnniv) { mois = i + 1 }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isNew) OutlinedButton(onClick = { confirm = true }, modifier = Modifier.height(50.dp)) { Text("Supprimer", color = Red) }
                Button(
                    onClick = { onSave(initial.copy(nom = nom.trim(), jour = j, mois = mois, annee = an)) },
                    enabled = ok, modifier = Modifier.weight(1f).height(50.dp),
                ) { Text("Enregistrer", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Supprimer cet anniversaire ?") },
        text = { Text(initial.nom) },
        confirmButton = { TextButton(onClick = { confirm = false; onDelete(initial) }) { Text("Supprimer", color = Red) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Annuler") } },
    )
}
