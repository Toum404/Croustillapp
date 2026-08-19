package fr.croustillapp.features.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import fr.croustillapp.R

/**
 * FR: Traduit dynamiquement le type d'etablissement reçu de l'API vers une ressource localisee.
 * EN: Dynamically maps the facility type string from the API onto a localized application resource.
 */
@Composable
@ReadOnlyComposable
fun getTranslationForType(type: String): String {
    // FR: Nettoyage preventif des espaces et normalisation en minuscules.
    // EN: Defensive whitespace trimming and lowercase casing normalization.
    return when (type.trim().lowercase()) {
        "tous", "toutes" -> stringResource(id = R.string.filtre_tous)
        "brasserie" -> stringResource(id = R.string.type_brasserie)
        "cafétéria" -> stringResource(id = R.string.type_cafeteria)
        "coffee" -> stringResource(id = R.string.type_coffee)
        "kiosque" -> stringResource(id = R.string.type_kiosque)
        "libre-service" -> stringResource(id = R.string.type_libre_service)
        "pizzéria" -> stringResource(id = R.string.type_pizzeria)
        "restaurant" -> stringResource(id = R.string.type_restaurant)
        "restaurant administratif" -> stringResource(id = R.string.type_resto_admin)
        "restaurant agréé" -> stringResource(id = R.string.type_resto_agree)
        "restaurant géré" -> stringResource(id = R.string.type_resto_gere)
        "sandwicherie" -> stringResource(id = R.string.type_sandwicherie)
        "triporteur" -> stringResource(id = R.string.type_triporteur)
        "épicerie" -> stringResource(id = R.string.type_epicerie)
        // FR: Repli securise renvoyant la chaine brute si le type n'est pas repertorie.
        // EN: Secure fallback returning the raw string value if the target type is unmapped.
        else -> type
    }
}