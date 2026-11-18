package com.koala.koalagenda

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Couleurs du thème, réglables (propres à chaque téléphone). */
object Theme {
    var fond by androidx.compose.runtime.mutableStateOf(Color(DEFAUT.getValue("fond")))
    var cases by androidx.compose.runtime.mutableStateOf(Color(DEFAUT.getValue("cases")))
    var accent by androidx.compose.runtime.mutableStateOf(Color(DEFAUT.getValue("accent")))
    var enfants by androidx.compose.runtime.mutableStateOf(Color(DEFAUT.getValue("enfants")))
    var ferie by androidx.compose.runtime.mutableStateOf(Color(DEFAUT.getValue("ferie")))
    var remplacement by androidx.compose.runtime.mutableStateOf(Color(DEFAUT.getValue("remplacement")))

    val DEFAUT: Map<String, Long>
        get() = mapOf(
            "fond" to 0xFF00071A, "cases" to 0xFF0D1733, "accent" to 0xFFFFCC00,
            "enfants" to 0xFF123B45, "ferie" to 0xFFFF9F43, "remplacement" to 0xFF3D8BFF,
        )

    val PRESETS: List<Pair<String, Map<String, Long>>>
        get() = listOf(
            "Nuit" to DEFAUT,
            "Forêt" to mapOf("fond" to 0xFF06120C, "cases" to 0xFF12291C, "accent" to 0xFFC6E06B, "enfants" to 0xFF3A3A12, "ferie" to 0xFFFF9F43, "remplacement" to 0xFF2ED3C6),
            "Prune" to mapOf("fond" to 0xFF12061A, "cases" to 0xFF2A1430, "accent" to 0xFFFF7EB6, "enfants" to 0xFF14385E, "ferie" to 0xFFFFCC00, "remplacement" to 0xFF5AA9FF),
            "Graphite" to mapOf("fond" to 0xFF0B0B0F, "cases" to 0xFF1E1E26, "accent" to 0xFF2ED3C6, "enfants" to 0xFF3D2A14, "ferie" to 0xFFFF6B6B, "remplacement" to 0xFFB57BFF),
        )

    /** Choix proposés pour chaque couleur. */
    val CHOIX: List<Triple<String, String, List<Long>>>
        get() = listOf(
            Triple("fond", "Fond", listOf(0xFF00071A, 0xFF0B0B0F, 0xFF06120C, 0xFF12061A, 0xFF101820, 0xFF1B1206, 0xFF06161F, 0xFF15151A)),
            Triple("cases", "Cases", listOf(0xFF0D1733, 0xFF1E1E26, 0xFF12291C, 0xFF2A1430, 0xFF1C2833, 0xFF2A1F10, 0xFF0D2A38, 0xFF2B2B33)),
            Triple("accent", "Accent", listOf(0xFFFFCC00, 0xFFFF9F43, 0xFFFF6B6B, 0xFFFF7EB6, 0xFFB57BFF, 0xFF5AA9FF, 0xFF2ED3C6, 0xFFC6E06B)),
            Triple("enfants", "Semaine enfants", listOf(0xFF123B45, 0xFF1E4D2B, 0xFF4A2350, 0xFF3A3A12, 0xFF14385E, 0xFF4D2A14, 0xFF0F4F4F, 0xFF3D1F1F)),
            Triple("ferie", "Férié", listOf(0xFFFF9F43, 0xFFFF6B6B, 0xFFFFCC00, 0xFFB57BFF, 0xFF2ED3C6, 0xFFFF7EB6, 0xFFC6E06B, 0xFF5AA9FF)),
            Triple("remplacement", "Ordo", listOf(0xFF3D8BFF, 0xFF2ED3C6, 0xFFB57BFF, 0xFFFF9F43, 0xFFFF6B6B, 0xFFC6E06B, 0xFFFF7EB6, 0xFFE0E6F5)),
        )

    fun valeur(cle: String): Long = when (cle) {
        "fond" -> fond; "cases" -> cases; "accent" -> accent
        "enfants" -> enfants; "ferie" -> ferie; else -> remplacement
    }.toArgb().toLong() and 0xFFFFFFFFL

    private fun appliquer(cle: String, v: Long) {
        val c = Color(v)
        when (cle) {
            "fond" -> fond = c; "cases" -> cases = c; "accent" -> accent = c
            "enfants" -> enfants = c; "ferie" -> ferie = c; "remplacement" -> remplacement = c
        }
    }

    private fun prefs(ctx: android.content.Context) = ctx.getSharedPreferences("theme", android.content.Context.MODE_PRIVATE)

    fun charger(ctx: android.content.Context) {
        val p = prefs(ctx)
        DEFAUT.forEach { (k, v) -> appliquer(k, p.getLong(k, v)) }
    }

    fun changer(ctx: android.content.Context, cle: String, v: Long) {
        appliquer(cle, v)
        prefs(ctx).edit().putLong(cle, v).apply()
    }

    fun preset(ctx: android.content.Context, m: Map<String, Long>) = m.forEach { (k, v) -> changer(ctx, k, v) }
}

val Navy: Color get() = Theme.fond
val Navy2: Color get() = Theme.cases
val Navy3: Color get() = androidx.compose.ui.graphics.lerp(Theme.cases, Color.White, 0.06f)
val Navy4: Color get() = androidx.compose.ui.graphics.lerp(Theme.cases, Color.White, 0.14f)
val Yellow: Color get() = Theme.accent
val Blue = Color(0xFF5AA9FF)
val Muted = Color(0xFFB7C0D8)
val Green = Color(0xFF4CC38A)
val Pink = Color(0xFFFF7EB6)
val Purple = Color(0xFFB57BFF)
val Red = Color(0xFFFF6B6B)

@Composable
fun KoalagendaTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Yellow, onPrimary = Navy, secondary = Blue, onSecondary = Navy,
        background = Navy, onBackground = Color.White, surface = Navy2, onSurface = Color.White,
        surfaceVariant = Navy3, onSurfaceVariant = Muted, surfaceContainer = Navy2,
        surfaceContainerHigh = Navy3, surfaceContainerHighest = Navy4,
        error = Red,
    ),
    content = content,
)

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

fun etatColor(e: Etat): Color = when (e) {
    Etat.TRAVAIL -> Navy4
    Etat.REPOS -> Green
    Etat.CONGE -> Yellow
    Etat.RECUP -> Blue
    Etat.FRAC -> Purple
}

/** Couleurs choisies par chacun (synchronisées), fournies par App. */
val LocalCouleurs = androidx.compose.runtime.compositionLocalOf<Map<Who, Long>> { COULEURS_DEFAUT }

val PALETTE: List<Long> = listOf(
    0xFF5AA9FF, 0xFF7C8CFF, 0xFFB57BFF, 0xFFFF7EB6, 0xFFFF6B6B,
    0xFFFF9F43, 0xFFC6E06B, 0xFF4CC38A, 0xFF2ED3C6, 0xFFE0E6F5,
)

@Composable
fun whoColor(w: Who): Color = Color(LocalCouleurs.current[w] ?: COULEURS_DEFAUT.getValue(w))

@Composable
fun pourColor(pour: String): Color = Who.of(pour)?.let { whoColor(it) } ?: Yellow

fun pourLabel(pour: String): String = when (pour) {
    "thomas" -> "Thomas"
    "cindy" -> "Cindy"
    else -> "Nous deux"
}

@Composable
fun Logo(size: Int = 22) = Row(verticalAlignment = Alignment.CenterVertically) {
    Image(
        painterResource(R.drawable.logo), null,
        Modifier.size((size * 1.6f).dp).clip(RoundedCornerShape((size * 0.35f).dp)),
    )
    Spacer(Modifier.width((size * 0.35f).dp))
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = Blue)) { append("Koal") }
            withStyle(SpanStyle(color = Yellow)) { append("agenda") }
        },
        fontWeight = FontWeight.Black, fontSize = size.sp,
    )
}

@Composable
fun Avatar(w: Who, size: Int = 34, onClick: (() -> Unit)? = null) = Box(
    Modifier.size(size.dp).clip(CircleShape).background(whoColor(w))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    contentAlignment = Alignment.Center,
) {
    Text(w.initiale, color = Navy, fontWeight = FontWeight.Black, fontSize = (size * 0.45f).sp)
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) = Row(
    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White) }
    Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp, modifier = Modifier.weight(1f))
    actions()
}

/** Pastille sélectionnable. */
@Composable
fun Choix(text: String, selected: Boolean, color: Color = Yellow, modifier: Modifier = Modifier, onClick: () -> Unit) = Box(
    modifier.clip(RoundedCornerShape(50))
        .background(if (selected) color else Navy3)
        .border(1.dp, if (selected) color else Navy4, RoundedCornerShape(50))
        .clickable { onClick() }
        .padding(horizontal = 14.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
) {
    Text(
        text, color = if (selected) Navy else Color.White,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp,
    )
}

@Composable
fun Section(title: String) = Text(
    title.uppercase(), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
)

@Composable
fun Carte(modifier: Modifier = Modifier, content: @Composable () -> Unit) = Column(
    modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Navy2).padding(14.dp),
) { content() }

@Composable
fun LoginScreen(repo: Repo, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Logo(34)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            email, { email = it }, label = { Text("E-mail commun") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            pass, { pass = it }, label = { Text("Mot de passe") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                busy = true
                scope.launch {
                    val r = runCatching { repo.signIn(email, pass) }
                    busy = false
                    r.onSuccess { onDone() }.onFailure {
                        val m = (it.message ?: "").lowercase()
                        toast(ctx, when {
                            "network" in m -> "Pas de connexion Internet"
                            "password" in m || "credential" in m || "no user" in m -> "E-mail ou mot de passe incorrect"
                            else -> "Connexion impossible : ${it.message}"
                        })
                    }
                }
            },
            enabled = !busy && email.isNotBlank() && pass.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Navy, strokeWidth = 2.dp)
            else Text("Se connecter", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun WhoScreen(onPick: (Who) -> Unit) = Column(
    Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Logo(30)
    Spacer(Modifier.height(36.dp))
    Text("Qui utilise ce téléphone ?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
    Spacer(Modifier.height(28.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Who.entries.forEach { w ->
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).background(Navy2).clickable { onPick(w) }.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(w, 64)
                Spacer(Modifier.height(10.dp))
                Text(w.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }
}

@Composable
fun CrashScreen(texte: String, onRetry: () -> Unit) {
    val ctx = LocalContext.current
    val upd = remember { UpdateState() }
    androidx.compose.runtime.LaunchedEffect(Unit) { upd.info = runCatching { Updater.check() }.getOrNull() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Logo(22)
        Spacer(Modifier.height(10.dp))
        UpdateBanner(upd)
        Text("L'appli a planté", color = Red, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text("Copie le rapport et envoie-le à Claude.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                val cm = ctx.getSystemService(android.content.ClipboardManager::class.java)
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("Koalagenda", texte))
                toast(ctx, "Rapport copié")
            }) { Text("Copier le rapport", fontWeight = FontWeight.Bold) }
            androidx.compose.material3.OutlinedButton(onClick = onRetry) { Text("Relancer", color = Yellow) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            texte, color = Color.White, fontSize = 11.sp,
            modifier = Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()),
        )
    }
}
