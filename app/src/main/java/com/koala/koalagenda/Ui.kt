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
import androidx.compose.ui.graphics.luminance
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
            "Océan" to mapOf("fond" to 0xFF03121C, "cases" to 0xFF0B2A3C, "accent" to 0xFF2ED3C6, "enfants" to 0xFF1E4D2B, "ferie" to 0xFFFF9F43, "remplacement" to 0xFF5AA9FF),
            "Coucher de soleil" to mapOf("fond" to 0xFF1A0A0A, "cases" to 0xFF33161A, "accent" to 0xFFFF9F43, "enfants" to 0xFF4A2350, "ferie" to 0xFFFFCC00, "remplacement" to 0xFFFF7EB6),
            "Lavande" to mapOf("fond" to 0xFF120F1F, "cases" to 0xFF241E3D, "accent" to 0xFFB57BFF, "enfants" to 0xFF14385E, "ferie" to 0xFFFF9F43, "remplacement" to 0xFF5AA9FF),
            "Cerise" to mapOf("fond" to 0xFF1A0508, "cases" to 0xFF3A1219, "accent" to 0xFFFF6B6B, "enfants" to 0xFF3D1F1F, "ferie" to 0xFFFFCC00, "remplacement" to 0xFFFF7EB6),
            "Menthe" to mapOf("fond" to 0xFF061A16, "cases" to 0xFF0F3330, "accent" to 0xFF4CC38A, "enfants" to 0xFF14385E, "ferie" to 0xFFFF9F43, "remplacement" to 0xFF2ED3C6),
            "Café" to mapOf("fond" to 0xFF140D07, "cases" to 0xFF2E2116, "accent" to 0xFFE0A33A, "enfants" to 0xFF3A3A12, "ferie" to 0xFFFF6B6B, "remplacement" to 0xFF5AA9FF),
            "Arctique" to mapOf("fond" to 0xFF0A1420, "cases" to 0xFF1A2A3D, "accent" to 0xFF9FD8FF, "enfants" to 0xFF123B45, "ferie" to 0xFFFF9F43, "remplacement" to 0xFF3D8BFF),
            "Électrique" to mapOf("fond" to 0xFF0B0B1A, "cases" to 0xFF1D1D3A, "accent" to 0xFFFFCC00, "enfants" to 0xFF3D1F1F, "ferie" to 0xFFFF6B6B, "remplacement" to 0xFF3D8BFF),
            "Néon" to mapOf("fond" to 0xFF05050A, "cases" to 0xFF151525, "accent" to 0xFFFF2E97, "enfants" to 0xFF0F4F4F, "ferie" to 0xFFC6E06B, "remplacement" to 0xFF00E5FF),
            "Ambre" to mapOf("fond" to 0xFF120B02, "cases" to 0xFF2B1D08, "accent" to 0xFFFFB000, "enfants" to 0xFF4D2A14, "ferie" to 0xFFFF6B6B, "remplacement" to 0xFF3D8BFF),
            "Émeraude" to mapOf("fond" to 0xFF03140C, "cases" to 0xFF0D2E1E, "accent" to 0xFF34E89E, "enfants" to 0xFF14385E, "ferie" to 0xFFFFCC00, "remplacement" to 0xFF5AA9FF),
            "Rubis" to mapOf("fond" to 0xFF15040A, "cases" to 0xFF33101E, "accent" to 0xFFFF4D6D, "enfants" to 0xFF4A2350, "ferie" to 0xFFFFCC00, "remplacement" to 0xFF3D8BFF),
            "Saphir" to mapOf("fond" to 0xFF040A1F, "cases" to 0xFF10204A, "accent" to 0xFF4D8DFF, "enfants" to 0xFF123B45, "ferie" to 0xFFFF9F43, "remplacement" to 0xFF2ED3C6),
            "Crépuscule" to mapOf("fond" to 0xFF0F0A1A, "cases" to 0xFF241A38, "accent" to 0xFFFF8A65, "enfants" to 0xFF14385E, "ferie" to 0xFFFFCC00, "remplacement" to 0xFFB57BFF),
            "Eucalyptus" to mapOf("fond" to 0xFF0E1412, "cases" to 0xFF1F2A27, "accent" to 0xFF9FC9A8, "enfants" to 0xFF2A3A4A, "ferie" to 0xFFFFCC00, "remplacement" to 0xFF5AA9FF),
            "Galaxie" to mapOf("fond" to 0xFF07041A, "cases" to 0xFF1A1240, "accent" to 0xFFE0B0FF, "enfants" to 0xFF0F4F4F, "ferie" to 0xFFFFCC00, "remplacement" to 0xFF2ED3C6),
            "Volcan" to mapOf("fond" to 0xFF120404, "cases" to 0xFF2E0E08, "accent" to 0xFFFF5722, "enfants" to 0xFF3D2A14, "ferie" to 0xFFFFCC00, "remplacement" to 0xFF3D8BFF),
            "Glacier" to mapOf("fond" to 0xFF061418, "cases" to 0xFF12303A, "accent" to 0xFF7FFFD4, "enfants" to 0xFF14385E, "ferie" to 0xFFFF9F43, "remplacement" to 0xFF9FD8FF),
            "Bonbon" to mapOf("fond" to 0xFF1A0614, "cases" to 0xFF3A1230, "accent" to 0xFFFFB3DE, "enfants" to 0xFF14385E, "ferie" to 0xFFFFCC00, "remplacement" to 0xFF9FD8FF),
            "Ardoise" to mapOf("fond" to 0xFF2E3440, "cases" to 0xFF3B4252, "accent" to 0xFF88C0D0, "enfants" to 0xFF4C566A, "ferie" to 0xFFD08770, "remplacement" to 0xFF81A1C1),
            "Brume" to mapOf("fond" to 0xFF4A5363, "cases" to 0xFF5A6476, "accent" to 0xFFFFD580, "enfants" to 0xFF3E6A6E, "ferie" to 0xFFFF9F43, "remplacement" to 0xFF9FD8FF),
            "Terracotta" to mapOf("fond" to 0xFF4E3029, "cases" to 0xFF623D34, "accent" to 0xFFFFB38A, "enfants" to 0xFF3C5A50, "ferie" to 0xFFFFD580, "remplacement" to 0xFF9FD8FF),
            "Olive" to mapOf("fond" to 0xFF3A4130, "cases" to 0xFF4A533D, "accent" to 0xFFE6D27A, "enfants" to 0xFF5A4636, "ferie" to 0xFFFF9F43, "remplacement" to 0xFF9FD8FF),
            "Bleu acier" to mapOf("fond" to 0xFF2A3F54, "cases" to 0xFF375068, "accent" to 0xFFFFC857, "enfants" to 0xFF2E5E5A, "ferie" to 0xFFFF8A65, "remplacement" to 0xFFB5E2FF),
            "Aubergine" to mapOf("fond" to 0xFF3D2A45, "cases" to 0xFF4D3657, "accent" to 0xFFFFB3DE, "enfants" to 0xFF2E5266, "ferie" to 0xFFFFD580, "remplacement" to 0xFF9FD8FF),
            "Pétrole" to mapOf("fond" to 0xFF1E4048, "cases" to 0xFF285159, "accent" to 0xFFFFD166, "enfants" to 0xFF4A3F5C, "ferie" to 0xFFFF8A65, "remplacement" to 0xFFB5E2FF),
            "Taupe" to mapOf("fond" to 0xFF4A423B, "cases" to 0xFF5A5149, "accent" to 0xFFF2C57C, "enfants" to 0xFF3E5A55, "ferie" to 0xFFFF8A65, "remplacement" to 0xFF9FD8FF),
            "Sauge" to mapOf("fond" to 0xFF9AAE9C, "cases" to 0xFFAFC0B0, "accent" to 0xFFE8B84A, "enfants" to 0xFFC4B48F, "ferie" to 0xFFC2410C, "remplacement" to 0xFF2F6FED),
            "Pierre" to mapOf("fond" to 0xFFA7A199, "cases" to 0xFFBAB4AC, "accent" to 0xFFE09A3E, "enfants" to 0xFF98B2B0, "ferie" to 0xFFB91C1C, "remplacement" to 0xFF2F6FED),
            "Lagon" to mapOf("fond" to 0xFF8DB0C2, "cases" to 0xFFA3C1D0, "accent" to 0xFFFFC857, "enfants" to 0xFFC9B48A, "ferie" to 0xFFC2410C, "remplacement" to 0xFF5B3FD9),
            "Vieux rose" to mapOf("fond" to 0xFFC29EA6, "cases" to 0xFFD0B1B8, "accent" to 0xFFE8B84A, "enfants" to 0xFFA3B8C9, "ferie" to 0xFFB91C1C, "remplacement" to 0xFF2F6FED),
            "Jour" to mapOf("fond" to 0xFFD9DEE7, "cases" to 0xFFE8EBF1, "accent" to 0xFFE0A100, "enfants" to 0xFFC6DDE0, "ferie" to 0xFFD9480F, "remplacement" to 0xFF2F6FED),
            "Lin" to mapOf("fond" to 0xFFE2DBCF, "cases" to 0xFFEEE8DE, "accent" to 0xFFC98A2E, "enfants" to 0xFFD3DEC6, "ferie" to 0xFFC2410C, "remplacement" to 0xFF3D7BD9),
            "Rosée" to mapOf("fond" to 0xFFE7D9DF, "cases" to 0xFFF1E7EB, "accent" to 0xFFD9658B, "enfants" to 0xFFD3DEEA, "ferie" to 0xFFE07800, "remplacement" to 0xFF4263EB),
            "Menthe claire" to mapOf("fond" to 0xFFD5E3DC, "cases" to 0xFFE4EEE9, "accent" to 0xFF22A57A, "enfants" to 0xFFECE0C2, "ferie" to 0xFFD9480F, "remplacement" to 0xFF1C7ED6),
            "Ciel" to mapOf("fond" to 0xFFD6E0EC, "cases" to 0xFFE5ECF4, "accent" to 0xFF3D8FD9, "enfants" to 0xFFECDEC6, "ferie" to 0xFFD9480F, "remplacement" to 0xFF7048E8),
            "Lavande claire" to mapOf("fond" to 0xFFDDD8EA, "cases" to 0xFFEAE6F3, "accent" to 0xFF8A6FD9, "enfants" to 0xFFCFE3DD, "ferie" to 0xFFE07800, "remplacement" to 0xFF1C7ED6),
            "Sable" to mapOf("fond" to 0xFFE3D9C6, "cases" to 0xFFEFE8DA, "accent" to 0xFFD9893A, "enfants" to 0xFFCFDCE6, "ferie" to 0xFFB91C1C, "remplacement" to 0xFF2F6FED),
            "Papier" to mapOf("fond" to 0xFFDADDE1, "cases" to 0xFFE8EAED, "accent" to 0xFFD9A000, "enfants" to 0xFFC9DDD4, "ferie" to 0xFFD9480F, "remplacement" to 0xFF1971C2),
            "Minuit doré" to mapOf("fond" to 0xFF05060F, "cases" to 0xFF14182E, "accent" to 0xFFE8C468, "enfants" to 0xFF123B45, "ferie" to 0xFFFF9F43, "remplacement" to 0xFF5AA9FF),
        )

    /** Choix proposés pour chaque couleur. */
    val CHOIX: List<Triple<String, String, List<Long>>>
        get() = listOf(
            Triple("fond", "Fond", listOf(0xFF2E3440, 0xFF4A5363, 0xFF4E3029, 0xFF2A3F54, 0xFF9AAE9C, 0xFF8DB0C2, 0xFFD9DEE7, 0xFFE2DBCF, 0xFFE7D9DF, 0xFFD5E3DC, 0xFF00071A, 0xFF0B0B0F, 0xFF06120C, 0xFF12061A, 0xFF101820, 0xFF1B1206, 0xFF06161F, 0xFF15151A)),
            Triple("cases", "Cases", listOf(0xFF3B4252, 0xFF5A6476, 0xFF623D34, 0xFF375068, 0xFFAFC0B0, 0xFFA3C1D0, 0xFFE8EBF1, 0xFFEEE8DE, 0xFFF1E7EB, 0xFF0D1733, 0xFF1E1E26, 0xFF12291C, 0xFF2A1430, 0xFF1C2833, 0xFF2A1F10, 0xFF0D2A38, 0xFF2B2B33)),
            Triple("accent", "Accent", listOf(0xFFFFCC00, 0xFFFF9F43, 0xFFFF6B6B, 0xFFFF7EB6, 0xFFB57BFF, 0xFF5AA9FF, 0xFF2ED3C6, 0xFFC6E06B)),
            Triple("enfants", "Semaine enfants", listOf(0xFF4C566A, 0xFF3E6A6E, 0xFF3C5A50, 0xFF2E5E5A, 0xFFC4B48F, 0xFF98B2B0, 0xFFC6DDE0, 0xFFD3DEC6, 0xFFECE0C2, 0xFFD3DEEA, 0xFF123B45, 0xFF1E4D2B, 0xFF4A2350, 0xFF3A3A12, 0xFF14385E, 0xFF4D2A14, 0xFF0F4F4F, 0xFF3D1F1F)),
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

/** Thème clair : fond lumineux, texte foncé. */
val Clair: Boolean get() = Theme.fond.luminance() > 0.25f
/** Texte principal (blanc sur thème sombre, foncé sur thème clair). */
val Encre: Color get() = if (Clair) Color(0xFF262B38) else Color.White
/** Texte posé sur une couleur vive (pastilles, carrés, boutons) : toujours foncé. */
val Sombre: Color get() = Color(0xFF0B0F1A)
val Navy: Color get() = Theme.fond
val Navy2: Color get() = Theme.cases
val Navy3: Color get() = androidx.compose.ui.graphics.lerp(Theme.cases, Encre, if (Clair) 0.05f else 0.06f)
val Navy4: Color get() = androidx.compose.ui.graphics.lerp(Theme.cases, Encre, if (Clair) 0.12f else 0.14f)
val Yellow: Color get() = Theme.accent
val Blue = Color(0xFF5AA9FF)
val Muted: Color get() = if (Clair) Color(0xFF626A7A) else Color(0xFFB7C0D8)
val Green = Color(0xFF4CC38A)
val Pink = Color(0xFFFF7EB6)
val Purple = Color(0xFFB57BFF)
val Red = Color(0xFFFF6B6B)

@Composable
fun KoalagendaTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = if (Clair) androidx.compose.material3.lightColorScheme(
        primary = Yellow, onPrimary = Sombre, secondary = Blue, onSecondary = Sombre,
        background = Navy, onBackground = Encre, surface = Navy2, onSurface = Encre,
        surfaceVariant = Navy3, onSurfaceVariant = Muted, surfaceContainer = Navy2,
        surfaceContainerHigh = Navy3, surfaceContainerHighest = Navy4,
        outline = Muted, error = Red,
    ) else darkColorScheme(
        primary = Yellow, onPrimary = Sombre, secondary = Blue, onSecondary = Sombre,
        background = Navy, onBackground = Encre, surface = Navy2, onSurface = Encre,
        surfaceVariant = Navy3, onSurfaceVariant = Muted, surfaceContainer = Navy2,
        surfaceContainerHigh = Navy3, surfaceContainerHighest = Navy4,
        outline = Muted, error = Red,
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
    0xFF5AA9FF, 0xFF3D8BFF, 0xFF7C8CFF, 0xFF4D5BFF, 0xFFB57BFF, 0xFF8A4DFF,
    0xFFE0B0FF, 0xFFFF7EB6, 0xFFFF2E97, 0xFFFF6B6B, 0xFFE03131, 0xFFFF8A65,
    0xFFFF9F43, 0xFFFFB000, 0xFFFFD166, 0xFFC6E06B, 0xFF94D82D, 0xFF4CC38A,
    0xFF20C997, 0xFF2ED3C6, 0xFF00B4D8, 0xFF9FD8FF, 0xFFA1887F, 0xFFE0E6F5,
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
    Text(w.initiale, color = Sombre, fontWeight = FontWeight.Black, fontSize = (size * 0.45f).sp)
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) = Row(
    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Encre) }
    Text(title, color = Encre, fontWeight = FontWeight.Black, fontSize = 19.sp, modifier = Modifier.weight(1f))
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
        text, color = if (selected) Sombre else Encre,
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
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Sombre, strokeWidth = 2.dp)
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
    Text("Qui utilise ce téléphone ?", color = Encre, fontWeight = FontWeight.Black, fontSize = 22.sp)
    Spacer(Modifier.height(28.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Who.entries.forEach { w ->
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).background(Navy2).clickable { onPick(w) }.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(w, 64)
                Spacer(Modifier.height(10.dp))
                Text(w.label, color = Encre, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
            texte, color = Encre, fontSize = 11.sp,
            modifier = Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()),
        )
    }
}
