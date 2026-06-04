package fr.croustillapp.core.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.croustillapp.R
import fr.croustillapp.ui.theme.Jersey10Family

// FR: Composant affiché lorsqu'aucun restaurant ne correspond aux filtres ou à la recherche.
// EN: Component displayed when no restaurants match the active filters or search queries.
@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    BaseErrorState(
        iconRes = R.drawable.ic_error_empty,
        title = stringResource(R.string.inconnu_titre),
        description = stringResource(R.string.inconnu_description),
        modifier = modifier
    )
}

// FR: Composant affiché en cas de perte de connexion réseau.
// EN: Component displayed when a network connection failure is detected.
@Composable
fun NoInternetState(modifier: Modifier = Modifier) {
    BaseErrorState(
        iconRes = R.drawable.ic_error_internet,
        title = stringResource(R.string.connexion_titre),
        description = stringResource(R.string.connexion_description),
        modifier = modifier
    )
}

// FR: Composant affiché en cas de problème ou de panne du serveur distant.
// EN: Component displayed during backend server issues or unreachable endpoints.
@Composable
fun ServerErrorState(modifier: Modifier = Modifier) {
    BaseErrorState(
        iconRes = R.drawable.ic_error_api,
        title = stringResource(R.string.serveur_titre),
        description = stringResource(R.string.serveur_description),
        modifier = modifier
    )
}

/**
 * FR: Modèle de base privé pour les écrans d'état/erreur, gérant nativement l'orientation de l'appareil.
 * EN: Private core layout blueprint for error/state screens, natively handling device screen orientation.
 */
@Composable
private fun BaseErrorState(
    iconRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    // FR: Détection dynamique de l'orientation de l'écran (Portrait vs Paysage).
    // EN: Dynamic runtime screen orientation detection (Portrait vs Landscape).
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val contentAlpha = 0.5f
    val commonTint = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)

    if (isLandscape) {
        // FR: Disposition horizontale optimisée pour le mode paysage afin d'éviter les coupures verticales.
        // EN: Horizontal layout optimized for landscape mode to prevent vertical clipping.
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = commonTint
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = Jersey10Family,
                    fontSize = 24.sp,
                    color = commonTint
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = commonTint
                )
            }
        }
    } else {
        // FR: Disposition verticale standard centrée pour le mode portrait.
        // EN: Standard centered vertical stack optimized for portrait viewing.
        Column(
            modifier = modifier
                .fillMaxSize()
                // FR: Évite que le contenu soit masqué ou chevauché lorsque le clavier virtuel (IME) apparaît.
                // EN: Prevents content overlap when the software keyboard (IME) becomes visible.
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = commonTint
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontFamily = Jersey10Family,
                fontSize = 24.sp,
                color = commonTint,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = commonTint,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}