package fr.croustillapp.main

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.croustillapp.ui.theme.CroustillappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentData: Uri? = intent?.data
        val initialRestaurantId = intentData?.let { uri ->
            extractRestaurantIdFromUrl(uri.toString())
        }
        val deepLinkUrlString = intentData?.toString()

        enableEdgeToEdge()

        setContent {
            CroustillappTheme {
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