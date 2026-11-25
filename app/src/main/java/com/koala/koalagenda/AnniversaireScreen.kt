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
import androidx.compose.foundation.layout.size
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

private val Violet = Color(0xFF9A58B0)

@Composable
fun AnniversaireEditScreen(
    initial: Anniversaire,
    isNew: Boolean,
    onSave: (Anniversaire) -> Unit,
    onDelete: (Anniversaire) -> Unit,
    onBack: () -> Unit,
) {
    var nom by remember { mutableStateOf(initial.nom) }
    var jour by remember { mutableStateOf(if (isNew) 0 else initial.jour) }
    var mois by remember { mutableStateOf(if (isNew) LocalDate.now().monthValue else initial.mois) }
    var annee by remember { mutableStateOf(if (initial.annee > 0) initial.annee.toString() else "") }
    var confirm by remember { mutableStateOf(false) }
    BackHandler { onBack() }

    val longueur = java.time.YearMonth.of(2024, mois).lengthOfMonth()
    if (jour > longueur) jour = longueur
    val an = annee.toIntOrNull() ?: 0
    val anOk = annee.isBlank() || an in 1900..LocalDate.now().year
    val ok = nom.isNotBlank() && jour > 0 && anOk
    val apercu = if (jour > 0) Anniversaire(nom = nom, mois = mois, jour = jour, annee = if (anOk) an else 0) else null

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        TopBar(if (isNew) "Nouvel anniversaire" else "Anniversaire", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Aperçu
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Violet.copy(alpha = 0.55f), Navy3)))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(R.drawable.ajout_anniv), null,
                    Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        nom.ifBlank { "Nom" }, color = if (nom.isBlank()) Muted else Encre,
                        fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (apercu != null) {
                        val prochain = apercu.prochain(LocalDate.now())
                        val age = apercu.age(prochain.year)
                        Text(
                            "${prochain.dayOfMonth} ${MOIS[mois - 1]}" + (age?.let { " · $it ans" } ?: ""),
                            color = Encre.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        )
                        val dans = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), prochain)
                        Text(
                            when (dans) { 0L -> "Aujourd'hui"; 1L -> "Demain"; else -> "Dans $dans jours" },
                            color = Muted, fontSize = 12.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(nom, { nom = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Etiquette("Mois")
            for (ligne in 0 until 4) {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (col in 0 until 3) {
                        val m = ligne * 3 + col + 1
                        val sel = m == mois
                        androidx.compose.foundation.layout.Box(
                            Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (sel) Violet else Navy2).clickable { mois = m },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                MOIS[m - 1].replaceFirstChar { it.uppercase() },
                                color = if (sel) Color.White else Encre, fontSize = 14.sp,
                                fontWeight = if (sel) FontWeight.Black else FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            Etiquette("Jour")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Navy2).padding(8.dp)) {
                for (ligne in 0 until 5) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0 until 7) {
                            val j = ligne * 7 + col + 1
                            androidx.compose.foundation.layout.Box(
                                Modifier.weight(1f).height(38.dp).padding(1.dp), contentAlignment = Alignment.Center,
                            ) {
                                if (j <= longueur) {
                                    val sel = j == jour
                                    androidx.compose.foundation.layout.Box(
                                        Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(if (sel) Violet else Color.Transparent).clickable { jour = j },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "$j", color = if (sel) Color.White else Encre, fontSize = 15.sp,
                                            fontWeight = if (sel) FontWeight.Black else FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Etiquette("Année de naissance")
            OutlinedTextField(
                annee, { annee = it.filter { c -> c.isDigit() }.take(4) },
                placeholder = { Text("Facultatif, pour l'âge") }, singleLine = true, isError = !anOk,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isNew) OutlinedButton(onClick = { confirm = true }, modifier = Modifier.height(52.dp)) { Text("Supprimer", color = Red) }
                Button(
                    onClick = { onSave(initial.copy(nom = nom.trim(), jour = jour, mois = mois, annee = an)) },
                    enabled = ok, modifier = Modifier.weight(1f).height(52.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Color.White),
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

@Composable
private fun Etiquette(t: String) = Text(
    t, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
)
