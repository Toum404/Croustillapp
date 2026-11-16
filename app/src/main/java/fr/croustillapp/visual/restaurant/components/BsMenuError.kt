package fr.croustillapp.visual.restaurant.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens

@Composable
fun MenuErrorView() {
    val errorIcons = listOf(
        R.drawable.ic_anim_internet_0,
        R.drawable.ic_anim_internet_1,
        R.drawable.ic_anim_internet_2,
        R.drawable.ic_anim_internet_3,
        R.drawable.ic_anim_internet_2,
        R.drawable.ic_anim_internet_1
    )

    val frameDuration = 1000
    val totalDuration = errorIcons.size * frameDuration

    val infiniteTransition = rememberInfiniteTransition(label = "menuErrorAnimation")

    val animatedValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = totalDuration.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = totalDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "errorIconIndex"
    )

    val currentIndex = (animatedValue / frameDuration).toInt().coerceIn(0, errorIcons.size - 1)

    BsSection(
        title = stringResource(id = R.string.bs_label_menu)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.bsSectionMenu),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = errorIcons[currentIndex]),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

            Text(
                text = stringResource(id = R.string.bs_erreur_menu),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}