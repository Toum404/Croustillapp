package fr.croustillapp.visual.restaurant.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens

@Composable
fun BsSectionAction(
    hasAddress: Boolean,
    hasPhone: Boolean,
    hasEmail: Boolean,
    showExtraActions: Boolean,
    labelAddress: String,
    labelPhone: String,
    labelEmail: String,
    labelTogglePlus: String,
    labelToggleMoins: String,
    labelBrowser: String,
    labelWidget: String,
    labelIcs: String,
    labelShare: String,
    onAddressClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onEmailClick: () -> Unit,
    onToggleClick: () -> Unit,
    onBrowserClick: () -> Unit,
    onWidgetClick: () -> Unit,
    onIcsClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionIconItem(
                iconRes = R.drawable.ic_act_adress,
                label = labelAddress,
                modifier = Modifier.weight(1f),
                enabled = hasAddress,
                onClick = onAddressClick
            )

            ActionIconItem(
                iconRes = R.drawable.ic_act_phone,
                label = labelPhone,
                modifier = Modifier.weight(1f),
                enabled = hasPhone,
                onClick = onPhoneClick
            )

            ActionIconItem(
                iconRes = R.drawable.ic_act_email,
                label = labelEmail,
                modifier = Modifier.weight(1f),
                enabled = hasEmail,
                onClick = onEmailClick
            )

            ActionIconItem(
                iconRes = if (showExtraActions) R.drawable.ic_toggle_up else R.drawable.ic_toggle_down,
                label = if (showExtraActions) labelToggleMoins else labelTogglePlus,
                modifier = Modifier.weight(1f),
                onClick = onToggleClick
            )
        }

        AnimatedVisibility(
            visible = showExtraActions,
            enter = fadeIn(animationSpec = tween(AppDimens.ANIMATION)) + expandVertically(animationSpec = tween(AppDimens.ANIMATION)),
            exit = fadeOut(animationSpec = tween(AppDimens.ANIMATION)) + shrinkVertically(animationSpec = tween(AppDimens.ANIMATION))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(AppDimens.spacerBig))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionIconItem(
                        iconRes = R.drawable.ic_act_online,
                        label = labelBrowser,
                        modifier = Modifier.weight(1f),
                        onClick = onBrowserClick
                    )

                    ActionIconItem(
                        iconRes = R.drawable.ic_act_widget,
                        label = labelWidget,
                        modifier = Modifier.weight(1f),
                        onClick = onWidgetClick
                    )

                    ActionIconItem(
                        iconRes = R.drawable.ic_act_ics,
                        label = labelIcs,
                        modifier = Modifier.weight(1f),
                        onClick = onIcsClick
                    )

                    ActionIconItem(
                        iconRes = R.drawable.ic_act_link,
                        label = labelShare,
                        modifier = Modifier.weight(1f),
                        onClick = onShareClick
                    )
                }
            }
        }
    }
}

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
            .clip(RoundedCornerShape(AppDimens.cornerSmall))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Icon(
            painter = if (enabled) painterResource(id = iconRes) else painterResource(R.drawable.ic_visual_false),
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}