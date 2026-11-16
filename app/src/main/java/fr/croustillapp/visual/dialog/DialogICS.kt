package fr.croustillapp.visual.dialog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import fr.croustillapp.R
import fr.croustillapp.data.model.Restaurant
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family
import fr.croustillapp.visual.components.CustomSwitch
import fr.croustillapp.visual.components.copyToClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogICS(
    title: String,
    onDismissRequest: () -> Unit,
    restaurant: Restaurant
) {
    val closeInteractionSource = remember { MutableInteractionSource() }
    val isClosePressed by closeInteractionSource.collectIsPressedAsState()

    val apiNom = stringResource(R.string.api_nom)

    var isMatinChecked by remember { mutableStateOf(false) }
    var isMidiChecked by remember { mutableStateOf(false) }
    var isSoirChecked by remember { mutableStateOf(false) }
    var isMinimalChecked by remember { mutableStateOf(false) }

    val isAnySelected = isMatinChecked || isMidiChecked || isSoirChecked

    val generateUrl: () -> String? = {
        val selectedRepas = buildList {
            if (isMatinChecked) add("matin")
            if (isMidiChecked) add("midi")
            if (isSoirChecked) add("soir")
        }

        val baseUrl = when {
            selectedRepas.isEmpty() -> null
            selectedRepas.size == 3 -> "https://api.croustillant.menu/calendar/${restaurant.id}.ics"
            else -> "https://api.croustillant.menu/calendar/${restaurant.id}.ics?repas=${selectedRepas.joinToString("%2C")}"
        }

        if (baseUrl != null && isMinimalChecked) {
            if (selectedRepas.size == 3) "$baseUrl?minimal=true" else "$baseUrl&minimal=true"
        } else {
            baseUrl
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    BasicAlertDialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppDimens.cornerSmall))
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = Jersey10Family,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeSelectCard(
                            title = stringResource(id = R.string.bs_colonne_matin),
                            iconRes = R.drawable.ic_ics_matin,
                            isChecked = isMatinChecked,
                            onClick = { isMatinChecked = !isMatinChecked },
                            modifier = Modifier.weight(1f)
                        )

                        TimeSelectCard(
                            title = stringResource(id = R.string.bs_colonne_midi),
                            iconRes = R.drawable.ic_ics_midi,
                            isChecked = isMidiChecked,
                            onClick = { isMidiChecked = !isMidiChecked },
                            modifier = Modifier.weight(1f)
                        )

                        TimeSelectCard(
                            title = stringResource(id = R.string.bs_colonne_soir),
                            iconRes = R.drawable.ic_ics_soir,
                            isChecked = isSoirChecked,
                            onClick = { isSoirChecked = !isSoirChecked },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDimens.cornerSmall))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { isMinimalChecked = !isMinimalChecked }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.dialog_minimal_titre),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(id = R.string.dialog_minimal_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        CustomSwitch(
                            checked = isMinimalChecked,
                            onCheckedChange = { isMinimalChecked = it }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.api_exclu, apiNom),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            val icsUrl = generateUrl()
                            if (icsUrl != null) {
                                val encodedIcsUrl = Uri.encode(icsUrl)
                                val googleCalendarUrl = "https://calendar.google.com/calendar/render?cid=$encodedIcsUrl"
                                val intent = Intent(Intent.ACTION_VIEW, googleCalendarUrl.toUri())
                                context.startActivity(intent)
                            }
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimens.cornerSmall),
                        enabled = isAnySelected
                    ) {
                        Text("Ajouter à Google Agenda")
                    }

                    Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

                    OutlinedButton(
                        onClick = {
                            val icsUrl = generateUrl()
                            if (icsUrl != null) {
                                copyToClipboard(scope, clipboard, context, icsUrl)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimens.cornerSmall)
                    ) {
                        Text("Copier le lien")
                    }

                    Spacer(modifier = Modifier.height(AppDimens.spacerMedium))
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_bs_close),
                    contentDescription = stringResource(id = R.string.bs_btn_1),
                    tint = if (isClosePressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                        .shadow(elevation = 2.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(
                            interactionSource = closeInteractionSource,
                            indication = LocalIndication.current
                        ) {
                            onDismissRequest()
                        }
                        .padding(AppDimens.paddingMedium)
                )
            }
        }
    }
}

@Composable
fun TimeSelectCard(
    title: String,
    iconRes: Int,
    isChecked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val backgroundColor = if (isChecked) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.background
    }

    val contentColor = if (isChecked) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    val borderColor = if (isChecked) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.cornerSmall))
            .background(backgroundColor)
            .border(
                width = if (isChecked) 0.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(AppDimens.cornerSmall)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(AppDimens.paddingMedium)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )

        Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_box_bg),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.matchParentSize()
            )

            Icon(
                painter = painterResource(
                    id = if (isChecked) R.drawable.ic_box_radio else R.drawable.ic_box_fg
                ),
                contentDescription = if (isChecked) stringResource(id = R.string.dialog_checkbox_on) else stringResource(id = R.string.dialog_checkbox_off),
                tint = if (isChecked) contentColor else MaterialTheme.colorScheme.outline,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}