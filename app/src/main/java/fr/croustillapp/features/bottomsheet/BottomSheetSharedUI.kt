package fr.croustillapp.features.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.croustillapp.R
import fr.croustillapp.features.data.JourOuvert
import fr.croustillapp.ui.theme.Jersey10Family
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * FR: Calcule une couleur de surface opaque en superposant une couleur transparente sur la couleur de fond de la feuille.
 * EN: Calculates an opaque surface color by compositing a semi-transparent layer over the sheet container's baseline background color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun getOpaqueSurfaceVariant(alpha: Float = 0.75f): Color {
    val sheetColor = BottomSheetDefaults.ContainerColor
    return MaterialTheme.colorScheme.surfaceVariant
        .copy(alpha = alpha)
        .compositeOver(sheetColor)
}

/**
 * FR: Élément de grille interactif affichant une icône d'action et un libellé textuel tronqué si nécessaire.
 * EN: Interactive grid item view displaying a contextual action icon alongside an optionally truncated text label.
 */
@Composable
fun ActionIconItem(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(getOpaqueSurfaceVariant(alpha = 0.5f))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

/**
 * FR: Décorateur pour ActionIconItem ajoutant un menu contextuel déroulant (DropdownMenu) lors du clic.
 * EN: Decorator layout wrapping an ActionIconItem to attach an anchor-bound popup DropdownMenu context interaction upon tapping.
 */
@Composable
fun ActionIconItemWithMenu(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    options: List<Triple<String, Int, () -> Unit>>
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ActionIconItem(
            iconRes = iconRes,
            label = label,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            onClick = { if (enabled) expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            options.forEach { (title, iconResId, action) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        action()
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        }
    }
}

/**
 * FR: Indicateur visuel d'état pour les caractéristiques de l'établissement (ex: PMR, Paiement Izly).
 * EN: Visual trait flag modifier presenting state details for facility features (e.g., PMR access, Izly payments).
 */
@Composable
fun FeatureIcon(
    iconRes: Int,
    label: String,
    isActive: Boolean
) {
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = contentColor
        )
    }
}

@Composable
fun VerticalDivider() {
    Spacer(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    )
}

/**
 * FR: Tableau structuré présentant les plages d'ouverture hebdomadaires (matin, midi, soir) avec mise en valeur du jour actuel.
 * EN: Matrix-style layout mapping weekly opening scopes (morning, noon, evening) highlighting the active current day.
 */
@Composable
fun PixelScheduleTable(joursOuverts: List<JourOuvert>) {
    // FR: Résolution de la clé temporelle ISO pour cibler et formater le jour courant de la semaine.
    // EN: Resolves ISO calendar timeline indices to match and distinctively highlight the active day of the week.
    val currentDayOfWeek = remember {
        LocalDate.now().dayOfWeek.value
    }

    val currentLocale = Locale.getDefault()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1.4f))
            Text(text = stringResource(id = R.string.colonne_matin), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(text = stringResource(id = R.string.colonne_midi), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(text = stringResource(id = R.string.colonne_soir), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }

        Spacer(modifier = Modifier.height(2.dp))

        joursOuverts.forEachIndexed { index, data ->
            val isToday = (index + 1) == currentDayOfWeek
            val textColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            val textStyle = if (isToday) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium

            val jourTraduit = remember(index, currentLocale) {
                DayOfWeek.of(index + 1)
                    .getDisplayName(TextStyle.FULL, currentLocale)
                    .replaceFirstChar { it.titlecase(currentLocale) }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1.4f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = jourTraduit,
                        style = textStyle.merge(
                            // FR: Désactivation forcée des marges de police système Android pour stabiliser l'alignement vertical.
                            // EN: Explicitly strips Android native font paddings to guarantee pixel-perfect baseline alignments.
                            androidx.compose.ui.text.TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        ),
                        fontFamily = if (isToday) Jersey10Family else null,
                        fontSize = if (isToday) 18.sp else 14.sp,
                        color = textColor
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    ScheduleStatusIcon(isOpen = data.ouverture.matin, isToday = isToday)
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    ScheduleStatusIcon(isOpen = data.ouverture.midi, isToday = isToday)
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    ScheduleStatusIcon(isOpen = data.ouverture.soir, isToday = isToday)
                }
            }
        }
    }
}

/**
 * FR: Icône unifiée matérialisant visuellement un état d'ouverture ou de fermeture d'un créneau donné.
 * EN: Consolidated status glyph dynamically rendering checkpoint checkmarks or blank spaces based on operational hours.
 */
@Composable
fun ScheduleStatusIcon(isOpen: Boolean, isToday: Boolean) {
    val iconRes = if (isOpen) R.drawable.ic_visual_check else R.drawable.ic_visual_empty
    val tintColor = when {
        isOpen && isToday -> MaterialTheme.colorScheme.primary
        isOpen -> MaterialTheme.colorScheme.primary.copy(alpha = 1f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    }

    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        tint = tintColor,
        modifier = Modifier.size(16.dp)
    )
}