package fr.croustillapp.visual.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens

@Composable
fun HomeSearchBar(
    searchText: String,
    isMinimalModeEnabled: Boolean,
    actionInteractionSource: MutableInteractionSource,
    onSearchTextChanged: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isActionPressed by actionInteractionSource.collectIsPressedAsState()

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(id = R.string.home_search), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_main_search),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            trailingIcon = {
                val hasText = searchText.isNotEmpty()
                val showSettings = !hasText && isMinimalModeEnabled
                val isClickable = hasText || showSettings

                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(48.dp)
                        .then(
                            if (isClickable) {
                                Modifier.clickable(
                                    indication = null,
                                    interactionSource = actionInteractionSource
                                ) {
                                    if (hasText) {
                                        onSearchTextChanged("")
                                    } else {
                                        onNavigateToSettings()
                                    }
                                }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconId = when {
                        hasText -> if (isActionPressed) R.drawable.ic_main_clear_on else R.drawable.ic_main_clear_off
                        showSettings -> if (isActionPressed) R.drawable.ic_bar_settings_on else R.drawable.ic_bar_settings_off
                        else -> R.drawable.ic_main_clear_off
                    }
                    val iconTint = if (!hasText && !showSettings) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else if (hasText) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Icon(
                        painter = painterResource(id = iconId),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            shape = RoundedCornerShape(AppDimens.cornerSmall),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            ),
            singleLine = true
        )
    }
}