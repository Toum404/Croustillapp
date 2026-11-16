package fr.croustillapp.data.helper

import fr.croustillapp.data.model.Restaurant

data class FilterUiParams(
    val query: String,
    val region: String,
    val type: String,
    val onlyOpen: Boolean,
    val onlyPmr: Boolean,
    val onlyIzly: Boolean
)

object RestaurantFilterHelper {

    fun filterAndSort(
        list: List<Restaurant>,
        params: FilterUiParams,
        favorites: Set<String>,
        newIds: Set<String>
    ): List<Restaurant> {
        val rawQuery = params.query.trim()
        val strictIdValue = extractStrictId(rawQuery)

        return list.filter { resto ->
            matchesSearch(resto, params.query, strictIdValue) && matchesFilters(resto, params)
        }.sortedWith(restaurantComparator(favorites, newIds))
    }

    private fun extractStrictId(rawQuery: String): String? {
        val isStrictIdSearch = rawQuery.startsWith("#") && rawQuery.drop(1).all { it.isDigit() }
        return if (isStrictIdSearch) rawQuery.drop(1) else null
    }

    private fun matchesSearch(resto: Restaurant, query: String, strictIdValue: String?): Boolean {
        return if (strictIdValue != null) {
            resto.id == strictIdValue
        } else {
            query.isEmpty() ||
                    resto.name.contains(query, ignoreCase = true) ||
                    resto.id.contains(query, ignoreCase = true)
        }
    }

    private fun matchesFilters(resto: Restaurant, params: FilterUiParams): Boolean {
        val matchesRegion = params.region == "Toutes" || resto.region == params.region
        val matchesType = params.type == "Tous" || resto.type == params.type
        val matchesOpen = !params.onlyOpen || resto.isOpen
        val matchesPmr = !params.onlyPmr || resto.pmr
        val matchesIzly = !params.onlyIzly || resto.acceptsIzly

        return matchesRegion && matchesType && matchesOpen && matchesPmr && matchesIzly
    }

    private fun restaurantComparator(favorites: Set<String>, newIds: Set<String>): Comparator<Restaurant> {
        val priorityComparator = Comparator.comparing<Restaurant, Int> { resto ->
            when {
                favorites.contains(resto.id) -> 0
                newIds.contains(resto.id) -> 1
                else -> 2
            }
        }

        val distanceOrNameComparator = Comparator<Restaurant> { r1, r2 ->
            if (r1.distance != null && r2.distance != null) {
                r1.distance.compareTo(r2.distance)
            } else {
                r1.name.compareTo(r2.name)
            }
        }

        return priorityComparator.thenComparing(distanceOrNameComparator)
    }
}