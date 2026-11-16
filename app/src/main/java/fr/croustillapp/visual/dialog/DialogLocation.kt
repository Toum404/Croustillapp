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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family
import fr.croustillapp.visual.restaurant.RestaurantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogLocation(
    onDismissRequest: () -> Unit,
    viewModel: RestaurantViewModel,
    onLocationSaved: () -> Unit
) {
    val closeInteractionSource = remember { MutableInteractionSource() }
    val isClosePressed by closeInteractionSource.collectIsPressedAsState()

    var latInput by remember { mutableStateOf("") }
    var lonInput by remember { mutableStateOf("") }

    val latDouble = latInput.toDoubleOrNull()
    val lonDouble = lonInput.toDoubleOrNull()
    val isValid = latDouble != null && latDouble in -90.0..90.0 && lonDouble != null && lonDouble in -180.0..180.0

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
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.dialog_tit_position),
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = Jersey10Family,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )

                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        label = { Text(stringResource(R.string.dialog_txt_lat)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(AppDimens.spacerBig))

                    OutlinedTextField(
                        value = lonInput,
                        onValueChange = { lonInput = it },
                        label = { Text(stringResource(R.string.dialog_txt_lon)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    latInput = ""
                                    lonInput = ""
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(AppDimens.cornerSmall)
                            ) {
                                Text(
                                    text = stringResource(R.string.dialog_btn_effacer),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    if (isValid) {
                                        viewModel.saveAndEnableCustomLocation(latDouble, lonDouble)
                                        onLocationSaved()
                                    }
                                },
                                enabled = isValid,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(AppDimens.cornerSmall),
                            ) {
                                Text(
                                    text = stringResource(R.string.dialog_btn_appliquer),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (isValid) {
                                    viewModel.saveAndEnableCustomLocation(latDouble, lonDouble)
                                    onLocationSaved()
                                }
                            },
                            enabled = isValid,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppDimens.cornerSmall),
                        ) {
                            Text(text = stringResource(R.string.dialog_btn_appliquer))
                        }

                        Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

                        OutlinedButton(
                            onClick = {
                                latInput = ""
                                lonInput = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppDimens.cornerSmall)
                        ) {
                            Text(text = stringResource(R.string.dialog_btn_effacer))
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