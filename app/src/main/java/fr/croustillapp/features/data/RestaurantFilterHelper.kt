package fr.croustillapp.features.data

/**
 * FR: Parametres UI utilises pour filtrer la liste des restaurants.
 * EN: UI parameters used to filter the restaurant list.
 */
data class FilterUiParams(
    val query: String,
    val region: String,
    val type: String,
    val onlyOpen: Boolean,
    val onlyPmr: Boolean
)

/**
 * FR: Utilitaire pur gerant le filtrage multi-criteres et le tri intelligent des restaurants.
 * EN: Pure utility class handling multi-criteria filtering and smart sorting of restaurants.
 */
object RestaurantFilterHelper {

    fun filterAndSort(
        list: List<Restaurant>,
        params: FilterUiParams
    ): List<Restaurant> {
        val rawQuery = params.query.trim()

        // Support d'une recherche stricte par ID prefixe par '#' (ex: #123) / Support strict ID search prefixed with '#' (e.g., #123)
        val isStrictIdSearch = rawQuery.startsWith("#") && rawQuery.drop(1).all { it.isDigit() }
        val strictIdValue = if (isStrictIdSearch) rawQuery.drop(1) else null

        return list.filter { resto ->
            val matchesSearch = if (strictIdValue != null) {
                resto.id == strictIdValue
            } else {
                params.query.isEmpty() ||
                        resto.name.contains(params.query, ignoreCase = true) ||
                        resto.id.contains(params.query, ignoreCase = true)
            }

            val matchesRegion = params.region == "Toutes" || resto.region == params.region
            val matchesType = params.type == "Tous" || resto.type == params.type
            val matchesOpen = !params.onlyOpen || resto.isOpen
            val matchesPmr = !params.onlyPmr || resto.pmr

            matchesSearch && matchesRegion && matchesType && matchesOpen && matchesPmr
        }.sortedWith { r1, r2 ->
            // Tri par distance si disponible, sinon tri alphabetique par nom / Sort by distance if available, otherwise sort alphabetically by name
            if (r1.distance != null && r2.distance != null) {
                r1.distance.compareTo(r2.distance)
            } else {
                r1.name.compareTo(r2.name)
            }
        }
    }
}