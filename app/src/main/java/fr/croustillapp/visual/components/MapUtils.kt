package fr.croustillapp.visual.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import fr.croustillapp.data.model.GeoJsonFeature
import fr.croustillapp.data.model.GeoJsonFeatureCollection
import fr.croustillapp.visual.main.TerritoryBound
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.cos

fun extractCoordinates(element: JsonElement, lons: MutableList<Double>, lats: MutableList<Double>) {
    try {
        if (element is JsonArray) {
            if (element.size == 2 && element[0].jsonPrimitive.isString.not() && element[1].jsonPrimitive.isString.not()) {
                try {
                    lons.add(element[0].jsonPrimitive.double)
                    lats.add(element[1].jsonPrimitive.double)
                    return
                } catch (_: Exception) {
                    // Ignore
                }
            }
            for (sub in element) {
                extractCoordinates(sub, lons, lats)
            }
        }
    } catch (_: Exception) {
        // Ignore
    }
}

fun createPathsFromGeometry(
    geometryElement: JsonElement,
    scale: Double,
    offsetX: Double,
    offsetY: Double,
    latCorrection: Double
): List<Path> {
    val polygons = extractIndependentPolygons(geometryElement)
    val allPaths = mutableListOf<Path>()
    for (polyRings in polygons) {
        allPaths.addAll(createPathsFromRings(polyRings, scale, offsetX, offsetY, latCorrection))
    }
    return allPaths
}

private fun isPolygonRings(element: JsonElement): Boolean {
    return element is JsonArray && element.all { it is JsonArray && isRing(it) }
}

private fun extractPolygonRings(element: JsonArray): List<List<JsonArray>> {
    return element.map { ringElement ->
        (ringElement as JsonArray).map { it as JsonArray }
    }
}

fun extractIndependentPolygons(geometryElement: JsonElement): List<List<List<JsonArray>>> {
    val result = mutableListOf<List<List<JsonArray>>>()
    try {
        if (geometryElement !is JsonArray || isRing(geometryElement)) return emptyList()

        if (isPolygonRings(geometryElement)) {
            return listOf(extractPolygonRings(geometryElement))
        }

        for (sub in geometryElement) {
            if (sub is JsonArray) {
                if (isPolygonRings(sub)) {
                    result.add(extractPolygonRings(sub))
                } else {
                    result.addAll(extractIndependentPolygons(sub))
                }
            }
        }
    } catch (_: Exception) {
        // Ignore
    }

    return result
}

fun createPathsFromRings(
    polygonRings: List<List<JsonArray>>,
    scale: Double,
    offsetX: Double,
    offsetY: Double,
    latCorrection: Double
): List<Path> {
    val paths = mutableListOf<Path>()
    try {
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
        }

        for (ring in polygonRings) {
            var first = true
            for (coord in ring) {
                try {
                    val lon = coord[0].jsonPrimitive.double
                    val lat = coord[1].jsonPrimitive.double

                    val x = ((lon * latCorrection) * scale + offsetX).toFloat()
                    val y = (offsetY - (lat * scale)).toFloat()

                    if (first) {
                        path.moveTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                    }
                } catch (_: Exception) {
                    // Ignore
                }
            }
            path.close()
        }
        paths.add(path)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return paths
}

fun isRing(array: JsonArray): Boolean {
    if (array.isEmpty()) return false
    val first = array[0]
    if (first !is JsonArray || first.size != 2) return false
    val subFirst = first[0]
    return subFirst !is JsonArray
}

/**
 * FR: Point d'entree pour projeter des coordonnees geographiques (lat/lon) en pixels sur le Canvas.
 * EN: Entry point for projecting geographic coordinates (lat/lon) into canvas pixels.
 */
fun mapUtils(
    lat: Double,
    lon: Double,
    canvasWidth: Float,
    canvasHeight: Float,
    geoJsonData: GeoJsonFeatureCollection?,
    overseasTerritories: List<TerritoryBound>
): Offset? {
    if (geoJsonData == null || canvasWidth <= 0f || canvasHeight <= 0f) return null

    val features = geoJsonData.features

    // FR: Facteur de correction de la longitude en fonction de la latitude moyenne de la France (46 degres).
    // EN: Longitude correction factor based on France's average latitude (46 degrees).
    val latCorrection = cos(Math.toRadians(46.0))

    // FR: On tente d'abord de projeter sur la France metropolitaine.
    // EN: Attempt projection on mainland France first.
    calculateMainlandOffset(lat, lon, canvasWidth, canvasHeight, features, latCorrection)?.let {
        return it
    }

    // FR: Sinon, on cherche parmi les territoires d'outre-mer.
    // EN: Otherwise, check among overseas territories.
    return calculateOverseasOffset(lat, lon, canvasWidth, canvasHeight, features, overseasTerritories, latCorrection)
}

/**
 * FR: Gere la projection des coordonnees pour la France metropolitaine avec calcul dynamique d'echelle.
 * EN: Handles coordinate projection for mainland France with dynamic scaling computation.
 */
private fun calculateMainlandOffset(
    lat: Double,
    lon: Double,
    canvasWidth: Float,
    canvasHeight: Float,
    features: List<GeoJsonFeature>,
    latCorrection: Double
): Offset? {
    val mainlandLons = mutableListOf<Double>()
    val mainlandLats = mutableListOf<Double>()

    // FR: Exclusion des regions d'outre-mer (crousId 8 et 29) pour isoler la metropole.
    // EN: Exclude overseas regions (crousId 8 and 29) to isolate mainland.
    features.forEach { feature ->
        val crousId = feature.properties.crousId
        if (crousId != 8 && crousId != 29) {
            val featureLons = mutableListOf<Double>()
            val featureLats = mutableListOf<Double>()
            extractCoordinates(feature.geometry.coordinates, featureLons, featureLats)
            mainlandLons.addAll(featureLons)
            mainlandLats.addAll(featureLats)
        }
    }

    if (mainlandLons.isEmpty() || mainlandLats.isEmpty()) return null

    val minLon = mainlandLons.minOrNull()!!
    val maxLon = mainlandLons.maxOrNull()!!
    val minLat = mainlandLats.minOrNull()!!
    val maxLat = mainlandLats.maxOrNull()!!

    val lonSpan = (maxLon - minLon) * latCorrection
    val latSpan = maxLat - minLat

    if (lonSpan <= 0 || latSpan <= 0) return null

    val scaleX = canvasWidth / lonSpan
    val scaleY = canvasHeight / latSpan
    val scale = minOf(scaleX, scaleY) * 0.80f

    val mapWidth = lonSpan * scale
    val mapHeight = latSpan * scale

    val offsetXAdjustment = canvasWidth * 0.08f
    val offsetX = (canvasWidth - mapWidth) / 2f - (minLon * latCorrection) * scale + offsetXAdjustment
    val offsetY = (canvasHeight + mapHeight) / 2f + minLat * scale

    // FR: Verification que le point GPS appartient bien aux limites de la metropole.
    // EN: Verify that the GPS point belongs within mainland boundaries.
    if (lon !in minLon..maxLon || lat !in minLat..maxLat) return null

    val x = (lon * latCorrection) * scale + offsetX
    val y = offsetY - (lat * scale)
    return Offset(x.toFloat(), y.toFloat())
}

/**
 * FR: Determine si le point appartient a un territoire d'outre-mer specifique et recupere sa configuration.
 * EN: Determines if the point belongs to a specific overseas territory and retrieves its configuration.
 */
private fun calculateOverseasOffset(
    lat: Double,
    lon: Double,
    canvasWidth: Float,
    canvasHeight: Float,
    features: List<GeoJsonFeature>,
    overseasTerritories: List<TerritoryBound>,
    latCorrection: Double
): Offset? {
    val vSize = canvasWidth * 0.08f
    val triangleCenterX = canvasWidth * 0.18f
    val triangleCenterY = canvasHeight * 0.46f
    val indianCenterX = canvasWidth * 0.24f
    val indianCenterY = canvasHeight * 0.88f

    // FR: Configuration des positions des vignettes d'outre-mer sur le canvas.
    // EN: Configuration of overseas vignette positions on the canvas.
    val overseasConfigs = listOf(
        Triple("guyane", vSize,
            Offset(triangleCenterX - canvasWidth * 0.06f, triangleCenterY + canvasHeight * 0.16f)
        ),
        Triple("guadeloupe", vSize,
            Offset(triangleCenterX - canvasWidth * 0.08f, triangleCenterY + canvasHeight * 0.06f)
        ),
        Triple("martinique", vSize,
            Offset(triangleCenterX - canvasWidth * 0.00f, triangleCenterY + canvasHeight * 0.08f)
        ),
        Triple("reunion", vSize, Offset(indianCenterX - canvasWidth * 0.02f, indianCenterY)),
        Triple("mayotte", vSize,
            Offset(indianCenterX - canvasWidth * 0.10f, indianCenterY - canvasHeight * 0.06f)
        )
    )

    for ((territoryKey, targetSize, targetCenter) in overseasConfigs) {
        val territory = overseasTerritories.find { it.key == territoryKey } ?: continue
        val (west, south, east, north) = territory.bounds

        if (lon in west..east && lat in south..north) {
            val matchedFeature = features.find { feature ->
                val crousId = feature.properties.crousId
                when (territoryKey) {
                    "guyane", "guadeloupe", "martinique" -> crousId == 8
                    "reunion", "mayotte" -> crousId == 29
                    else -> false
                }
            } ?: continue

            val offset = computeVignetteOffset(lat, lon,
                matchedFeature, territory, targetSize, targetCenter, latCorrection)
            if (offset != null) return offset
        }
    }
    return null
}

/**
 * FR: Calcule le decalage precis a l'interieur de la vignette d'outre-mer avec recentrage par bounding box.
 * EN: Computes the precise offset inside the overseas vignette with bounding box recentering.
 */
private fun computeVignetteOffset(
    lat: Double,
    lon: Double,
    feature: GeoJsonFeature,
    territory: TerritoryBound,
    targetSize: Float,
    targetCenter: Offset,
    latCorrection: Double
): Offset? {
    val (west, south, east, north) = territory.bounds
    val independentPolys = extractIndependentPolygons(feature.geometry.coordinates)

    for (polyRings in independentPolys) {
        val firstCoord = polyRings.firstOrNull()?.firstOrNull() ?: continue
        if (firstCoord.size < 2) continue

        try {
            val pLng = firstCoord[0].jsonPrimitive.double
            val pLat = firstCoord[1].jsonPrimitive.double
            if (pLng !in west..east || pLat !in south..north) continue

            val tLons = mutableListOf<Double>()
            val tLats = mutableListOf<Double>()
            polyRings.forEach { ring ->
                ring.forEach { coord ->
                    tLons.add(coord[0].jsonPrimitive.double)
                    tLats.add(coord[1].jsonPrimitive.double)
                }
            }

            val tMinLon = tLons.minOrNull()!!
            val tMaxLon = tLons.maxOrNull()!!
            val tMinLat = tLats.minOrNull()!!
            val tMaxLat = tLats.maxOrNull()!!

            val tLonSpan = (tMaxLon - tMinLon) * latCorrection
            val tLatSpan = tMaxLat - tMinLat
            if (tLonSpan <= 0 || tLatSpan <= 0) continue

            val localScale = minOf(targetSize / tLonSpan, targetSize / tLatSpan)
            val tOffsetX = -(tMinLon * latCorrection) * localScale
            val tOffsetY = tMaxLat * localScale

            val rawX = (lon * latCorrection) * localScale + tOffsetX
            val rawY = tOffsetY - (lat * localScale)

            val vignettePaths = createPathsFromRings(polyRings, localScale, tOffsetX, tOffsetY, latCorrection)
            var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE

            vignettePaths.forEach { path ->
                val bounds = path.getBounds()
                minX = minOf(minX, bounds.left)
                maxX = maxOf(maxX, bounds.right)
                minY = minOf(minY, bounds.top)
                maxY = maxOf(maxY, bounds.bottom)
            }

            if (minX < maxX && minY < maxY) {
                val currentCenterX = (minX + maxX) / 2f
                val currentCenterY = (minY + maxY) / 2f
                val deltaX = targetCenter.x - currentCenterX
                val deltaY = targetCenter.y - currentCenterY

                return Offset((rawX + deltaX).toFloat(), (rawY + deltaY).toFloat())
            }
        } catch (_: Exception) {
            // Ignore
        }
    }
    return null
}