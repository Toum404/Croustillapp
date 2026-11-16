package fr.croustillapp.visual.dialog

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family
import fr.croustillapp.visual.components.copyToClipboard
import fr.croustillapp.visual.components.shareText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogContact(
    onDismissRequest: () -> Unit,
    title: String,
    contact: String,
    error: String,
    isButton1Enabled: Boolean = false,
    button1Text: String,
    onButton1Click: () -> Unit
) {
    val closeInteractionSource = remember { MutableInteractionSource() }
    val isClosePressed by closeInteractionSource.collectIsPressedAsState()

    val copyInteractionSource = remember { MutableInteractionSource() }
    val isCopyPressed by copyInteractionSource.collectIsPressedAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimens.cornerSmall),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.paddingBig)
                        ) {
                            SelectionContainer {
                                Text(
                                    text = contact,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                IconButton(
                                    onClick = {
                                        copyToClipboard(
                                            scope = scope,
                                            clipboard = clipboard,
                                            context = context,
                                            text = contact
                                        )
                                    },
                                    interactionSource = copyInteractionSource,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (isCopyPressed) R.drawable.ic_copy_on else R.drawable.ic_copy_off
                                        ),
                                        contentDescription = stringResource(R.string.copie_action),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isButton1Enabled) 0f else 1f)
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    shareText(context, contact)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(AppDimens.cornerSmall)
                            ) {
                                Text(
                                    text = stringResource(R.string.dialog_btn_partager),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = onButton1Click,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(AppDimens.cornerSmall),
                                enabled = isButton1Enabled
                            ) {
                                Text(
                                    text = button1Text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onButton1Click,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppDimens.cornerSmall),
                            enabled = isButton1Enabled
                        ) {
                            Text(text = button1Text)
                        }

                        Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

                        OutlinedButton(
                            onClick = {
                                shareText(context, contact)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppDimens.cornerSmall)
                        ) {
                            Text(text = stringResource(R.string.dialog_btn_partager))
                        }
                    }
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