package fr.croustillapp.core.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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

/**
 * Écran d'état vide affiché lorsqu'aucun restaurant ou donnée n'est disponible.
 * Empty state screen displayed when no restaurants or data are found.
 */
@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    BaseErrorState(
        iconRes = R.drawable.ic_inconnu,
        title = stringResource(R.string.inconnu_titre),
        description = stringResource(R.string.inconnu_description),
        modifier = modifier
    )
}

/**
 * Écran d'erreur affiché en cas de perte de connexion Internet.
 * Connection error state screen displayed when network access is unavailable.
 */
@Composable
fun NoInternetState(modifier: Modifier = Modifier) {
    BaseErrorState(
        iconRes = R.drawable.ic_connexion,
        title = stringResource(R.string.connexion_titre),
        description = stringResource(R.string.connexion_description),
        modifier = modifier
    )
}

/**
 * Composant squelette privé encapsulant la gestion adaptative de l'orientation (Portrait/Paysage).
 * Private layout wrapper encapsulating fluid orientation adaptive behaviors (Portrait/Landscape).
 */
@Composable
private fun BaseErrorState(
    iconRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val contentAlpha = 0.5f
    val commonTint = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)

    if (isLandscape) {
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
                modifier = Modifier.size(100.dp),
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 64.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
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