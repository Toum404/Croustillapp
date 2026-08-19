package fr.croustillapp.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import fr.croustillapp.R
import fr.croustillapp.main.MainActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * FR: Widget principal affichant le menu du restaurant du jour.
 * EN: Main widget displaying today's restaurant menu.
 */
class RestaurantMenuWidget : GlanceAppWidget() {

    // Utilise DataStore pour la gestion des etats du widget / Uses DataStore for widget state management
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Formate la date actuelle pour l'URL de l'API / Formats the current date for the API URL
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val todayDate = LocalDate.now().format(formatter)

        provideContent {
            val prefs = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()
            val restaurantCodeKey = stringPreferencesKey("restaurant_code")

            // FR: Recupere le code du restaurant
            // EN: Retrieves the restaurant code
            val localCode = prefs[restaurantCodeKey]
            val restaurantCode = if (!localCode.isNullOrBlank()) {
                localCode
            } else {
                val sharedPrefs = context.getSharedPreferences("restaurant_widget_prefs", Context.MODE_PRIVATE)
                sharedPrefs.getString("last_selected_restaurant_id", "000") ?: "000"
            }

            // Construit l'URL de l'image du menu / Constructs the menu image URL
            val imageUrl = "https://api.croustillant.menu/v1/restaurants/$restaurantCode/menu/$todayDate/image"

            GlanceTheme {
                RestaurantWidgetContent(imageUrl = imageUrl, restaurantCode = restaurantCode)
            }
        }
    }
}

/**
 * FR: Composant Composable gerant l'affichage graphique du widget.
 * EN: Composable component handling the widget's graphical layout.
 */
@Composable
fun RestaurantWidgetContent(imageUrl: String, restaurantCode: String) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Action declenchee au clic sur le widget / Action triggered when clicking the widget
    val clickableAction = actionRunCallback<OpenAppAndRefreshAction>()

    // Chargement de l'image du menu via Coil de maniere asynchrone
    // Asynchronous loading of the menu image using Coil
    LaunchedEffect(imageUrl) {
        try {
            val loader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                if (drawable is BitmapDrawable) {
                    bitmap = drawable.bitmap
                } else {
                    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 500
                    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 500
                    val bMap = createBitmap(width, height)
                    val canvas = android.graphics.Canvas(bMap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap = bMap
                }
            } else {
                errorMessage = "HTTP"
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "404"
        }
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(8.dp)
            .clickable(clickableAction),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Cas 1 : Aucun restaurant selectionne / Case 1: No restaurant selected
            restaurantCode == "000" -> {
                Text(
                    text = context.getString(R.string.widget_info),
                    style = TextStyle(
                        textAlign = TextAlign.Center,
                        color = GlanceTheme.colors.onBackground
                    )
                )
            }
            // Cas 2 : Image chargee avec succes / Case 2: Image loaded successfully
            bitmap != null -> {
                Image(
                    provider = ImageProvider(bitmap!!),
                    contentDescription = "Widget",
                    modifier = GlanceModifier.fillMaxSize()
                )
            }
            // Cas 3 : Erreur de chargement / Case 3: Loading error
            errorMessage != null -> {
                Text(
                    text = context.getString(R.string.widget_erreur),
                    style = TextStyle(
                        textAlign = TextAlign.Center,
                        color = GlanceTheme.colors.onBackground
                    )
                )
            }
            // Cas 4 : En cours de chargement / Case 4: Loading in progress
            else -> {
                Text(
                    text = context.getString(R.string.widget_chargement),
                    style = TextStyle(
                        textAlign = TextAlign.Center,
                        color = GlanceTheme.colors.onBackground
                    )
                )
            }
        }
    }
}

/**
 * FR: Gere l'action de clic : ouvre l'application et rafraichit le widget.
 * EN: Handles click action: opens the app and refreshes the widget.
 */
class OpenAppAndRefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = context.getSharedPreferences("restaurant_widget_prefs", Context.MODE_PRIVATE)
        val restaurantCode = prefs.getString("last_selected_restaurant_id", "000") ?: "000"

        // FR: Redirige vers l'ecran principal ou directement vers le restaurant si configure
        // EN: Redirects to main screen or directly to the restaurant if configured
        val intent = if (restaurantCode == "000") {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = "https://croustillant.menu/r/$restaurantCode".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        context.startActivity(intent)

        RestaurantMenuWidget().update(context, glanceId)
    }
}

/**
 * FR: Recepteur Broadcast pour initialiser le widget Android Glance.
 * EN: Broadcast receiver to initialize the Android Glance widget.
 */
class RestaurantWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RestaurantMenuWidget()
}