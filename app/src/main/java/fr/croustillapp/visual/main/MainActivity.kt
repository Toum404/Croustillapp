package fr.croustillapp.visual.main

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.croustillapp.ui.theme.CroustillappTheme
import fr.croustillapp.visual.restaurant.RestaurantViewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: RestaurantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentData: Uri? = intent?.data
        val initialRestaurantId = intentData?.let { uri ->
            extractRestaurantIdFromUrl(uri.toString())
        }
        val deepLinkUrlString = intentData?.toString()

        enableEdgeToEdge()

        setContent {
            val themeOption by viewModel.themeOption.collectAsStateWithLifecycle()
            val isDynamicColorEnabled by viewModel.isDynamicColorEnabled.collectAsStateWithLifecycle()
            val isPureBlackEnabled by viewModel.isPureBlackEnabled.collectAsStateWithLifecycle()

            val darkTheme = when (themeOption) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window
                    window?.let {
                        WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !darkTheme
                    }
                }
            }

            CroustillappTheme(
                darkTheme = darkTheme,
                dynamicColor = isDynamicColorEnabled,
                isPureBlackEnabled = isPureBlackEnabled
            ) {
                MainScreen(
                    initialRestaurantId = initialRestaurantId,
                    deepLinkUrl = deepLinkUrlString
                )
            }
        }
    }
}

/**
 * FR: Extrait l'ID unique du restaurant depuis l'URL de l'application.
 * EN: Extracts the unique venue ID from deep-linked application context URLs.
 */
fun extractRestaurantIdFromUrl(url: String?): String? {
    if (url == null) return null

    val regex = Regex("-?r/?(\\d+)")
    val matchResult = regex.find(url)

    val id = matchResult?.groupValues?.getOrNull(1)

    return id
}