package fr.croustillapp.visual.restaurant.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.BottomSheetButtonClose(
    scope: CoroutineScope,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val closeInteractionSource = remember { MutableInteractionSource() }
    val isClosePressed by closeInteractionSource.collectIsPressedAsState()

    Icon(
        painter = painterResource(id = R.drawable.ic_bs_close),
        contentDescription = stringResource(id = R.string.bs_btn_1),
        tint = if (isClosePressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(AppDimens.paddingBig)
            .size(36.dp)
            .shadow(elevation = 2.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(
                interactionSource = closeInteractionSource,
                indication = LocalIndication.current
            ) {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) onDismiss()
                }
            }
            .padding(AppDimens.paddingMedium)
    )
}