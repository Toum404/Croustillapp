package fr.croustillapp.visual.dialog

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import fr.croustillapp.R
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.ui.theme.Jersey10Family
import fr.croustillapp.extra.RestaurantWidgetReceiver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogWidget(
    onDismissRequest: () -> Unit,
    restaurantId: String
) {
    val closeInteractionSource = remember { MutableInteractionSource() }
    val isClosePressed by closeInteractionSource.collectIsPressedAsState()

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }

    val context = LocalContext.current

    var widgetButtonState by remember { mutableIntStateOf(0) }

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
                        text = stringResource(R.string.dialog_tit_widget),
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = Jersey10Family,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 320.dp, height = 160.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        scope.launch {
                                            rotationX.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 300f))
                                        }
                                        scope.launch {
                                            rotationY.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 300f))
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            rotationX.snapTo((rotationX.value - dragAmount.y * 0.2f).coerceIn(-15f, 15f))
                                            rotationY.snapTo((rotationY.value + dragAmount.x * 0.2f).coerceIn(-15f, 15f))
                                        }
                                    }
                                )
                            }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    this.rotationX = rotationX.value
                                    this.rotationY = rotationY.value
                                    cameraDistance = 16.dp.toPx() * density
                                },
                            shape = RoundedCornerShape(AppDimens.cornerSmall),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_widget),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimens.spacerBig))

                    Button(
                        onClick = {
                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val myWidgetProvider = ComponentName(context, RestaurantWidgetReceiver::class.java)

                            val sharedPrefs = context.getSharedPreferences("restaurant_widget_prefs", Context.MODE_PRIVATE)
                            sharedPrefs.edit {
                                putString("last_selected_restaurant_id", restaurantId)
                            }

                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                appWidgetManager.requestPinAppWidget(myWidgetProvider, null, null)
                                widgetButtonState = 1
                            } else {
                                widgetButtonState = 2
                            }
                        },
                        enabled = widgetButtonState == 0,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimens.cornerSmall)
                    ) {
                        val buttonTextRes = when (widgetButtonState) {
                            1 -> R.string.dialog_act_widget_2
                            2 -> R.string.dialog_act_widget_0
                            else -> R.string.dialog_act_widget_1
                        }
                        Text(text = stringResource(id = buttonTextRes))
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