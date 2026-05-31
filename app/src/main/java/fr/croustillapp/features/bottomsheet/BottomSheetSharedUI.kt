package fr.croustillapp.features.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Couleur calculée avec compositeOver pour éviter les conflits de superposition avec le bandeau des jours fériés.
// Unified background because of conflicts with holidays alert if just an alpha on surfaceVariant.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun getOpaqueSurfaceVariant(alpha: Float = 0.75f): Color {
    val sheetColor = BottomSheetDefaults.ContainerColor
    return MaterialTheme.colorScheme.surfaceVariant
        .copy(alpha = alpha)
        .compositeOver(sheetColor)
}

// Bouton de cellule standard pour les actions rapides -> dropdown menu pour prévénir les faux positifs.
// Standard utility quick action grid cell button -> dropdown menu to prevent missclick.
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

// Composant d'action avancé qui déploie un DropdownMenu contextuel lors d'un clic.
// Advanced action grid cell component that opens an overlay contextual DropdownMenu when pressed.
@Composable
fun ActionIconItemWithMenu(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    options: List<Pair<String, () -> Unit>>
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
            options.forEach { (title, action) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        action()
                        expanded = false
                    }
                )
            }
        }
    }
}

// Indicateur d'icône pour les métadonnées changeant de couleur selon l'activation de la fonctionnalité.
// Metadata icon label indicator designed to switch tint configurations based on local availability states.
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

// Ligne de séparation verticale discrète utilisée pour compartimenter les lignes de métadonnées.
// Discrete micro vertical hair line divider used to partition layout metadata rows.
@Composable
fun VerticalDivider() {
    Spacer(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    )
}