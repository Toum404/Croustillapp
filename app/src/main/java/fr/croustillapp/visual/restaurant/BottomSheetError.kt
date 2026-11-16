package fr.croustillapp.visual.restaurant

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family
import fr.croustillapp.visual.main.extractRestaurantIdFromUrl
import fr.croustillapp.visual.restaurant.components.BottomSheetButtonClose
import fr.croustillapp.visual.restaurant.components.BottomSheetHeaderContainer
import kotlinx.coroutines.launch

/**
 * FR: Version modifiee de la bottomsheet de base pour afficher les erreurs de DeepLink
 * EN: Modified base bottom sheet layout mapping deep link errors
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantErrorBottomSheet(
    message: String,
    deepLinkUrl: String,
    onDismiss: () -> Unit,
    onInjectIdToSearch: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val sheetColor = MaterialTheme.colorScheme.background

    val searchActionInteractionSource = remember { MutableInteractionSource() }
    val isSearchActionPressed by searchActionInteractionSource.collectIsPressedAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        modifier = Modifier.statusBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetHeaderContainer(
                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                    sheetColor = sheetColor
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(R.drawable.img_error)
                            .decoderFactory(ImageDecoderDecoder.Factory())
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.error_restaurant_tit),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = Jersey10Family,
                        fontSize = 36.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(AppDimens.spacerMedium))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = 300.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            BottomSheetButtonClose(scope = scope, sheetState = sheetState, onDismiss = onDismiss)

            Icon(
                painter = painterResource(
                    id = R.drawable.ic_main_search
                ),
                contentDescription = stringResource(R.string.error_solution),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppDimens.paddingBig)
                    .size(36.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(
                        interactionSource = searchActionInteractionSource,
                        indication = LocalIndication.current
                    ) {
                        val extractedId = extractRestaurantIdFromUrl(deepLinkUrl)
                        if (!extractedId.isNullOrEmpty()) {
                            scope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                onDismiss()
                                onInjectIdToSearch(extractedId)
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = if (isSearchActionPressed) 0.85f else 1.0f,
                        scaleY = if (isSearchActionPressed) 0.85f else 1.0f,
                        alpha = if (isSearchActionPressed) 0.7f else 1.0f
                    )
                    .padding(AppDimens.paddingMedium)
            )
        }
    }
}