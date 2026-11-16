package fr.croustillapp.data.model

data class UiMenu(
    val date: String,
    val repas: List<UiRepas>
)

data class UiRepas(
    val type: String,
    val categories: List<UiCategory>
)

data class UiCategory(
    val libelle: String,
    val plats: List<UiPlat>
)

data class UiPlat(
    val libelle: String,
    val points: String?
)