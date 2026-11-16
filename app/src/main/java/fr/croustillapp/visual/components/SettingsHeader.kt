package fr.croustillapp.visual.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family

@Composable
fun SettingsHeader(
    isLandscape: Boolean,
    isMinimalModeEnabled: Boolean,
    initialHeight: Dp,
    minHeight: Dp,
    surfaceCollapseOffset: Float,
    maxCollapsePx: Float,
    onCloseToHomeClick: () -> Unit,
    urlGitHub: String,
    urlDiscord: String,
    urlApp: String,
    urlSite: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progress = if (maxCollapsePx > 0f) (surfaceCollapseOffset / maxCollapsePx) else 0f

    val animatedHeight = if (!isLandscape) {
        initialHeight - ((initialHeight - minHeight) * progress)
    } else {
        initialHeight
    }

    val closeInteractionSource = remember { MutableInteractionSource() }
    val isClosePressed by closeInteractionSource.collectIsPressedAsState()

    val erreurAction = stringResource(R.string.url_action_erreur)
    val openUrl = remember(context, erreurAction) {
        { url: String ->
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, erreurAction, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.cornerSmall),
        color = Color.Transparent
    ) {
        Column(
            modifier = if (isLandscape) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(animatedHeight)
                    .heightIn(min = minHeight)
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary
                            ),
                            start = Offset(Float.POSITIVE_INFINITY, 0f),
                            end = Offset(0f, Float.POSITIVE_INFINITY)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MinimalModeButton(
                        isMinimalModeEnabled = isMinimalModeEnabled,
                        isClosePressed = isClosePressed,
                        interactionSource = closeInteractionSource,
                        onClick = onCloseToHomeClick
                    )

                    GitHubButton(urlGitHub = urlGitHub)
                }

                HeaderAppTitle(isLandscape = isLandscape, progress = progress)
            }

            SettingsFooterSection(
                isLandscape = isLandscape,
                urlDiscord = urlDiscord,
                urlApp = urlApp,
                urlSite = urlSite,
                openUrl = openUrl
            )
        }
    }
}

@Composable
private fun MinimalModeButton(
    isMinimalModeEnabled: Boolean,
    isClosePressed: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
) {
    if (isMinimalModeEnabled) {
        Row(
            modifier = Modifier
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(50.dp))
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                ) {
                    onClick()
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isClosePressed) R.drawable.ic_bar_home_on else R.drawable.ic_bar_home_off
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.navbar_2_home),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    } else {
        Spacer(modifier = Modifier.width(1.dp))
    }
}

@Composable
private fun GitHubButton(urlGitHub: String) {
    val context = LocalContext.current
    val gitInteractionSource = remember { MutableInteractionSource() }
    val isGitPressed by gitInteractionSource.collectIsPressedAsState()

    Icon(
        painter = painterResource(
            id = if (isGitPressed) R.drawable.ic_bs_git_on else R.drawable.ic_bs_git_off
        ),
        contentDescription = urlGitHub,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .shadow(elevation = 2.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = gitInteractionSource,
                indication = LocalIndication.current
            ) {
                try {
                    val webIntent = Intent.makeMainSelectorActivity(
                        Intent.ACTION_MAIN,
                        Intent.CATEGORY_APP_BROWSER
                    ).apply {
                        data = urlGitHub.toUri()
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                } catch (_: Exception) {
                    Toast.makeText(context, R.string.dialog_non_web, Toast.LENGTH_SHORT).show()
                }
            }
            .padding(8.dp)
            .size(20.dp)
    )
}

@Composable
private fun HeaderAppTitle(isLandscape: Boolean, progress: Float) {
    val textAlpha = if (!isLandscape) {
        (1f - (progress * 1.5f)).coerceIn(0f, 1f)
    } else {
        1f
    }

    if (textAlpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .alpha(textAlpha),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_version),
                    color = MaterialTheme.colorScheme.background,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.app_name),
                    color = MaterialTheme.colorScheme.background,
                    fontFamily = Jersey10Family,
                    fontSize = 50.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsFooterSection(
    isLandscape: Boolean,
    urlDiscord: String,
    urlApp: String,
    urlSite: String,
    openUrl: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.api_mention),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.api_nom),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(if (isLandscape) AppDimens.spacerMedium else AppDimens.spacerBig),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsContentButtons(
                    label = stringResource(R.string.info_api_discord),
                    iconOutlinedRes = R.drawable.ic_api_discord_off,
                    iconFilledRes = R.drawable.ic_api_discord_on,
                    onClick = { openUrl(urlDiscord) }
                )

                SettingsContentButtons(
                    label = stringResource(R.string.info_api_app),
                    iconOutlinedRes = R.drawable.ic_api_app_off,
                    iconFilledRes = R.drawable.ic_api_app_on,
                    onClick = { openUrl(urlApp) }
                )

                SettingsContentButtons(
                    label = stringResource(R.string.info_api_website),
                    iconOutlinedRes = R.drawable.ic_api_site_off,
                    iconFilledRes = R.drawable.ic_api_site_on,
                    onClick = { openUrl(urlSite) }
                )
            }
        }
    }
}