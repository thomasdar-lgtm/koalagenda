package com.koala.koalagenda

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlinx.coroutines.launch

/* ---------- Congés / récups / fracs sur une période ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeriodeScreen(
    state: AgendaState,
    moi: Who,
    debut: LocalDate,
    onApply: (List<Jour>) -> Unit,
    onBack: () -> Unit,
) {
    var who by remember { mutableStateOf(moi) }
    var etat by remember { mutableStateOf<Etat?>(Etat.CONGE) }
    var du by remember { mutableStateOf(debut) }
    var au by remember { mutableStateOf(debut) }
    BackHandler { onBack() }
    val changes = poserPeriode(state.roulement(who), state.jours, who, du, au, etat, System.currentTimeMillis())

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        TopBar("Poser sur une période", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Section("Pour")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Who.entries.forEach { w -> Choix(w.label, who == w, whoColor(w)) { who = w } }
            }
            Section("Quoi")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Etat.CONGE, Etat.RECUP, Etat.FRAC, Etat.REPOS, Etat.TRAVAIL).forEach { e ->
                    Choix(e.label, etat == e, whoColor(who)) { etat = e }
                }
                Choix("Auto", etat == null, Muted) { etat = null }
            }
            Section("Période")
            DateChamp("Du", du) { du = it; if (au.isBefore(it)) au = it }
            Spacer(Modifier.height(8.dp))
            DateChamp("Au (inclus)", au) { au = it; if (it.isBefore(du)) du = it }
            Spacer(Modifier.height(14.dp))
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onApply(changes) }, enabled = changes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text("Appliquer · ${changes.size} j", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ---------- Réglage des roulements ---------- */

@Composable
fun RoulementScreen(state: AgendaState, moi: Who, onSave: (Who, Roulement) -> Unit, onBack: () -> Unit) {
    var who by remember { mutableStateOf(moi) }
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        TopBar("Roulements", onBack)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Who.entries.forEach { w -> Choix(w.label, who == w, whoColor(w)) { who = w } }
        }
        EditeurRoulement(who, state.roulement(who), onSave = { onSave(who, it) })
    }
}

@Composable
private fun JoursChips(sel: Set<Int>, couleur: Color, onToggle: (Int) -> Unit) = Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    JOURS_COURTS.forEachIndexed { i, l ->
        val j = i + 1
        val on = j in sel
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(if (on) couleur else Navy3).clickable { onToggle(j) },
            contentAlignment = Alignment.Center,
        ) { Text(l, color = if (on) Sombre else Encre, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
    }
}

@Composable
private fun EditeurRoulement(who: Who, r0: Roulement, onSave: (Roulement) -> Unit) {
    val today = LocalDate.now()
    var fixes by remember(who, r0) { mutableStateOf(r0.fixes) }
    var cycle by remember(who, r0) { mutableStateOf(r0.cycle) }
    var actuelle by remember(who, r0) { mutableIntStateOf(r0.semaine(today).coerceAtLeast(0)) }
    var conges by remember(who, r0) { mutableStateOf(r0.congesParAn.toString()) }

    fun construit() = Roulement(fixes, cycle, r0.debut, conges.toIntOrNull() ?: r0.congesParAn)
        .let { if (cycle.isEmpty()) it else it.avecSemaineActuelle(today, actuelle.coerceIn(0, cycle.size - 1)) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Section("Repos fixes")
        JoursChips(fixes, whoColor(who)) { j -> fixes = if (j in fixes) fixes - j else fixes + j }

        Section("Repos tournant")
        cycle.forEachIndexed { i, s ->
            Carte(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Semaine ${i + 1}", color = Encre, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Choix("En cours", actuelle == i) { actuelle = i }
                }
                Spacer(Modifier.height(8.dp))
                JoursChips(s, whoColor(who)) { j ->
                    cycle = cycle.toMutableList().also { it[i] = if (j in s) s - j else s + j }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { cycle = cycle + listOf(emptySet()) }) { Text("+ Semaine", color = Yellow) }
            if (cycle.isNotEmpty()) OutlinedButton(onClick = {
                cycle = cycle.dropLast(1)
                if (actuelle >= cycle.size) actuelle = (cycle.size - 1).coerceAtLeast(0)
            }) { Text("− Dernière semaine", color = Red) }
        }

        Section("Congés")
        OutlinedTextField(
            conges, { conges = it.filter { c -> c.isDigit() }.take(3) },
            label = { Text("Congés par an") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Section("Aperçu")
        val apercu = construit()
        val l0 = lundi(today)
        repeat(4) { k ->
            val l = l0.plusWeeks(k.toLong())
            val repos = (0..6).map { l.plusDays(it.toLong()) }.filter { apercu.etatAuto(it) == Etat.REPOS }
            Text(
                "Sem. du ${l.dayOfMonth}/${l.monthValue} : " + repos.joinToString(", ") { JOURS_LONGS[it.dayOfWeek.value - 1] },
                color = Encre, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = { onSave(construit()) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Enregistrer", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

/* ---------- Compteurs ---------- */

@Composable
fun CompteursScreen(state: AgendaState, onBack: () -> Unit) {
    var annee by remember { mutableIntStateOf(LocalDate.now().year) }
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        TopBar("Compteurs", onBack)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { annee-- }) { Icon(Icons.Filled.KeyboardArrowLeft, "Année précédente", tint = Encre) }
            Text(
                "$annee", color = Encre, fontWeight = FontWeight.Black, fontSize = 20.sp,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { annee++ }) { Icon(Icons.Filled.KeyboardArrowRight, "Année suivante", tint = Encre) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Who.entries.forEach { w ->
                val c = compter(state.jours.values, w, annee)
                val quota = state.roulement(w).congesParAn
                Carte(Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(w, 28)
                        Spacer(Modifier.width(10.dp))
                        Text(w.label, color = Encre, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Ligne("Congés posés", "${c.conges} / $quota", Yellow)
                    Ligne("Congés restants", "${quota - c.conges}", Yellow)
                    Ligne("Récups", "${c.recups}", Blue)
                    Ligne("Fracs", "${c.fracs}", Purple)
                    Ligne("Heures sup", "${c.heuresSup}", Encre)
                    Ligne("Remplacements", "${c.remplacements.size}", Encre)
                    c.remplacements.forEach { (d, p) ->
                        Text("· ${fmtCourt(d)} : $p", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Ligne(label: String, valeur: String, color: Color) = Row(
    Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically,
) {
    Text(label, color = Muted, fontSize = 15.sp, modifier = Modifier.weight(1f))
    Text(valeur, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
}

/* ---------- Réglages ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReglagesScreen(
    moi: Who,
    upd: UpdateState,
    onRoulements: () -> Unit,
    onCompteurs: () -> Unit,
    onChangeMoi: () -> Unit,
    onSignOut: () -> Unit,
    onCouleur: (Who, Long) -> Unit,
    enfants: Enfants,
    onEnfants: (Enfants) -> Unit,
    onBack: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var cherche by remember { mutableStateOf(false) }
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        TopBar("Réglages", onBack)
        UpdateBanner(upd)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Section("Application")
            Carte {
                Ligne("Version", BuildConfig.VERSION_NAME, Encre)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        cherche = true
                        scope.launch {
                            runCatching { Updater.check() }
                                .onSuccess { i -> upd.info = i; if (i == null) toast(ctx, "Koalagenda est à jour (${BuildConfig.VERSION_NAME})") }
                                .onFailure { toast(ctx, "Recherche impossible, vérifie ta connexion") }
                            cherche = false
                        }
                    },
                    enabled = !cherche, modifier = Modifier.fillMaxWidth(),
                ) { Text(if (cherche) "Recherche…" else "Rechercher une mise à jour", fontWeight = FontWeight.Bold) }
            }
            Section("Planning")
            LienReglage("Roulements", onRoulements)
            LienReglage("Compteurs", onCompteurs)
            Section("Enfants")
            Carte {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Une semaine sur deux", color = Encre, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(checked = enfants.actif, onCheckedChange = { onEnfants(enfants.copy(actif = it)) })
                }
                if (enfants.actif) {
                    Spacer(Modifier.height(10.dp))
                    val ici = enfants.presents(LocalDate.now())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choix("Cette semaine : avec", ici, Color(0xFF2ED3C6)) { onEnfants(enfants.avec(LocalDate.now(), true)) }
                        Choix("sans", !ici, Muted) { onEnfants(enfants.avec(LocalDate.now(), false)) }
                    }
                }
            }
            Section("Couleurs")
            Who.entries.forEach { w -> ChoixCouleur(w, onCouleur) }
            Section("Thème")
            Presets(ctx)
            Spacer(Modifier.height(10.dp))
            Theme.CHOIX.forEach { (cle, nom, options) ->
                val actuelle = Theme.valeur(cle)
                Carte(Modifier.padding(bottom = 8.dp)) {
                    Text(nom, color = Encre, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { c ->
                            Box(
                                Modifier.size(28.dp).clip(CircleShape).background(Color(c))
                                    .border(if (c == actuelle) 3.dp else 1.dp, if (c == actuelle) Encre else Muted.copy(alpha = 0.3f), CircleShape)
                                    .clickable { Theme.changer(ctx, cle, c) },
                            )
                        }
                    }
                }
            }
            Section("Ce téléphone")
            LienReglage("Je suis ${moi.label} · changer", onChangeMoi)
            LienReglage("Se déconnecter", onSignOut)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LienReglage(titre: String, onClick: () -> Unit) = Column(
    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
        .background(Navy2).clickable { onClick() }.padding(14.dp),
) {
    Text(titre, color = Encre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

@Composable
private fun ChoixCouleur(w: Who, onCouleur: (Who, Long) -> Unit) {
    val actuelle = LocalCouleurs.current[w]
    Carte(Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(w, 26)
            Spacer(Modifier.width(10.dp))
            Text(w.label, color = Encre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PALETTE.forEach { c ->
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(Color(c))
                        .then(if (c == actuelle) Modifier.border(3.dp, Encre, CircleShape) else Modifier)
                        .clickable { onCouleur(w, c) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Presets(ctx: android.content.Context) = FlowRow(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Theme.PRESETS.forEach { (nom, m) ->
        val actif = m.all { (k, v) -> Theme.valeur(k) == v }
        Row(
            Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(Color(m.getValue("fond")))
                .border(if (actif) 2.dp else 1.dp, if (actif) Encre else Color(m.getValue("cases")), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .clickable { Theme.preset(ctx, m) }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("cases", "accent", "enfants").forEach { k ->
                Box(Modifier.padding(end = 3.dp).size(12.dp).clip(CircleShape).background(Color(m.getValue(k))))
            }
            Spacer(Modifier.width(4.dp))
            Text(nom, color = Encre, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
