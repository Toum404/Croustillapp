package fr.croustillapp.features.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import fr.croustillapp.R

@Composable
@ReadOnlyComposable
fun getTranslationForType(type: String): String {
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
        else -> type
    }
}