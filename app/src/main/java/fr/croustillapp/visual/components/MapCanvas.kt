package fr.croustillapp.visual.components

import android.graphics.RectF
import android.graphics.Region
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.croustillapp.R
import fr.croustillapp.data.model.GeoJsonFeature
import fr.croustillapp.data.model.GeoJsonFeatureCollection
import fr.croustillapp.ui.theme.AppDimens
import fr.croustillapp.visual.main.TerritoryBound
import fr.croustillapp.visual.restaurant.RestaurantViewModel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.cos

// FR: Typealias pour structurer proprement les donnees precalculees de la carte.
// EN: Typealias to cleanly structure precalculated map data.
typealias MapPathsData = Pair<List<Triple<Int, GeoJsonFeature, List<Path>>>, List<Triple<Int, GeoJsonFeature, List<Path>>>>

/**
 * FR: Composant principal gerant le rendu de la carte interactive, le zoom et le pan.
 * EN: Main component managing interactive map rendering, zooming, and panning.
 */
@Composable
fun MapCanvas(
    modifier: Modifier = Modifier,
    viewModel: RestaurantViewModel,
    geoJsonData: GeoJsonFeatureCollection?,
    isLoading: Boolean,
    userLocation: Pair<Double, Double>?,
    selectedCrousId: Int?,
    overseasTerritories: List<TerritoryBound>
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedBackgroundColor = MaterialTheme.colorScheme.primary

    val regionColor = MaterialTheme.colorScheme.background
    val borderColor = MaterialTheme.colorScheme.outline

    val pointerBorder = MaterialTheme.colorScheme.background
    val pointerBody = MaterialTheme.colorScheme.secondary

    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val isZoomButtons by viewModel.isZoomButtons.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(AppDimens.cornerSmall))
            .background(backgroundColor)
    ) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()
        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

        // FR: Precalcul des chemins graphiques pour eviter de les recalculer a chaque frame.
        // EN: Precalculation of graphic paths to avoid recalculating them on every frame.
        val precalculatedMap = remember(canvasWidth, canvasHeight, geoJsonData) {
            if (geoJsonData == null || canvasWidth <= 0f || canvasHeight <= 0f) return@remember null
            calculatePrecalculatedMap(geoJsonData, canvasWidth, canvasHeight, overseasTerritories)
        }

        val currentZoom by rememberUpdatedState(zoom)
        val currentPan by rememberUpdatedState(panOffset)
        val currentMap by rememberUpdatedState(precalculatedMap)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        handleMapTap(
                            tapOffset = tapOffset,
                            activeZoom = currentZoom,
                            activePan = currentPan,
                            center = center,
                            activeMap = currentMap,
                            onCrousSelected = { crousId -> viewModel.toggleCrousSelection(crousId) }
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(1f, AppDimens.MAP_MAX_ZOOM)
                        val panFactor = (zoom - 1f) * 0.6f
                        val maxPanX = canvasWidth * panFactor
                        val maxPanY = canvasHeight * panFactor
                        val newX = (panOffset.x + panChange.x).coerceIn(-maxPanX, maxPanX)
                        val newY = (panOffset.y + panChange.y).coerceIn(-maxPanY, maxPanY)
                        panOffset = Offset(newX, newY)
                    }
                }
        ) {
            when {
                isLoading -> MapLoadingIndicator(borderColor)
                geoJsonData == null -> MapErrorState()
                else -> MapCanvasContent(
                    precalculatedMap = precalculatedMap,
                    panOffset = panOffset,
                    zoom = zoom,
                    center = center,
                    selectedCrousId = selectedCrousId,
                    selectedBackgroundColor = selectedBackgroundColor,
                    regionColor = regionColor,
                    borderColor = borderColor,
                    userLocation = userLocation,
                    geoJsonData = geoJsonData,
                    overseasTerritories = overseasTerritories,
                    pointerBorder = pointerBorder,
                    pointerBody = pointerBody
                )
            }
        }

        if (isZoomButtons) {
            MapZoomControls(
                zoom = zoom,
                modifier = Modifier.align(Alignment.BottomCenter),
                onZoomChanged = { newZoom -> zoom = newZoom },
                onPanReset = { panOffset = Offset.Zero }
            )
        }
    }
}

/**
 * FR: Convertit les coordonnees du clic ecran en coordonnees monde pour detecter la region selectionnee.
 * EN: Converts screen tap coordinates into world coordinates to detect the selected region.
 */
private fun handleMapTap(
    tapOffset: Offset,
    activeZoom: Float,
    activePan: Offset,
    center: Offset,
    activeMap: MapPathsData?,
    onCrousSelected: (Int) -> Unit
) {
    val unpannedX = tapOffset.x - activePan.x
    val unpannedY = tapOffset.y - activePan.y
    val unscaledX = center.x + (unpannedX - center.x) / activeZoom
    val unscaledY = center.y + (unpannedY - center.y) / activeZoom
    val clickPoint = Offset(unscaledX, unscaledY)

    activeMap?.let { (mainland, overseas) ->
        if (findAndSelectClickedRegion(mainland, clickPoint, onCrousSelected)) return
        findAndSelectClickedRegion(overseas, clickPoint, onCrousSelected)
    }
}

/**
 * FR: Utilise les regions Android natives pour verifier si le point clique intersecte un chemin vectoriel.
 * EN: Uses native Android regions to check if the tapped point intersects a vector path.
 */
private fun findAndSelectClickedRegion(
    regions: List<Triple<Int, GeoJsonFeature, List<Path>>>,
    clickPoint: Offset,
    onCrousSelected: (Int) -> Unit
): Boolean {
    for ((crousId, _, paths) in regions) {
        if (checkIntersection(paths, clickPoint)) {
            onCrousSelected(crousId)
            return true
        }
    }
    return false
}

private fun checkIntersection(paths: List<Path>, clickPoint: Offset): Boolean {
    return paths.any { composePath ->
        val androidPath = composePath.asAndroidPath()
        val bounds = RectF()
        androidPath.computeBounds(bounds, true)

        val region = Region().apply {
            setPath(
                androidPath,
                Region(
                    bounds.left.toInt(),
                    bounds.top.toInt(),
                    bounds.right.toInt(),
                    bounds.bottom.toInt()
                )
            )
        }
        region.contains(clickPoint.x.toInt(), clickPoint.y.toInt())
    }
}

@Composable
private fun MapLoadingIndicator(borderColor: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = borderColor)
    }
}

@Composable
private fun MapErrorState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_map_error),
            contentDescription = stringResource(R.string.carte_erreur),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * FR: Dessine les elements graphiques de la carte (metropole, outre-mer, position utilisateur) dans le Canvas.
 * EN: Draws graphical map elements (mainland, overseas, user location) inside the Canvas.
 */
@Composable
private fun MapCanvasContent(
    precalculatedMap: MapPathsData?,
    panOffset: Offset,
    zoom: Float,
    center: Offset,
    selectedCrousId: Int?,
    selectedBackgroundColor: Color,
    regionColor: Color,
    borderColor: Color,
    userLocation: Pair<Double, Double>?,
    geoJsonData: GeoJsonFeatureCollection,
    overseasTerritories: List<TerritoryBound>,
    pointerBorder: Color,
    pointerBody: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        precalculatedMap?.let { (mainland, overseas) ->
            withTransform({
                translate(panOffset.x, panOffset.y)
                scale(zoom, zoom, pivot = center)
            }) {
                val strokeWidth = 2f / zoom

                drawRegionPaths(mainland, selectedCrousId, selectedBackgroundColor, regionColor, borderColor, strokeWidth)
                drawRegionPaths(overseas, selectedCrousId, selectedBackgroundColor, regionColor, borderColor, strokeWidth)

                userLocation?.let { (lat, lon) ->
                    val mapContext = MapContext(
                        canvasWidth = size.width,
                        canvasHeight = size.height,
                        geoJsonData = geoJsonData,
                        overseasTerritories = overseasTerritories
                    )
                    drawUserLocationPoint(lat, lon, zoom, mapContext, pointerBorder, pointerBody)
                }
            }
        }
    }
}

private fun DrawScope.drawRegionPaths(
    regions: List<Triple<Int, GeoJsonFeature, List<Path>>>,
    selectedCrousId: Int?,
    selectedBackgroundColor: Color,
    regionColor: Color,
    borderColor: Color,
    strokeWidth: Float
) {
    regions.forEach { (crousId, _, paths) ->
        val isSelected = (crousId == selectedCrousId)
        val fillColor = if (isSelected) selectedBackgroundColor else regionColor
        paths.forEach { path ->
            drawPath(path = path, color = fillColor)
            drawPath(path = path, color = borderColor, style = Stroke(width = strokeWidth))
        }
    }
}

private data class MapContext(
    val canvasWidth: Float,
    val canvasHeight: Float,
    val geoJsonData: GeoJsonFeatureCollection,
    val overseasTerritories: List<TerritoryBound>
)

private fun DrawScope.drawUserLocationPoint(
    lat: Double,
    lon: Double,
    zoom: Float,
    mapContext: MapContext,
    pointerBorder: Color,
    pointerBody: Color
) {
    val pointOffset = mapUtils(
        lat = lat,
        lon = lon,
        canvasWidth = mapContext.canvasWidth,
        canvasHeight = mapContext.canvasHeight,
        geoJsonData = mapContext.geoJsonData,
        overseasTerritories = mapContext.overseasTerritories
    )

    pointOffset?.let { offset ->
        drawCircle(
            color = pointerBody.copy(alpha = 0.5f),
            radius = 14f / zoom,
            center = offset
        )

        drawCircle(
            color = pointerBorder,
            radius = 9f / zoom,
            center = offset
        )

        drawCircle(
            color = pointerBody,
            radius = 6.5f / zoom,
            center = offset
        )
    }
}

@Composable
private fun MapZoomControls(
    zoom: Float,
    modifier: Modifier = Modifier,
    onZoomChanged: (Float) -> Unit,
    onPanReset: () -> Unit
) {
    val minZoom = 1f
    val maxZoom = AppDimens.MAP_MAX_ZOOM
    val step = (maxZoom - minZoom) / 2f
    val canZoomOut = zoom > minZoom
    val canZoomIn = zoom < maxZoom

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(
                topStart = AppDimens.cornerSmall,
                topEnd = AppDimens.cornerSmall,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            ),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 6.dp
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZoomButton(
                    isZoomIn = false,
                    enabled = canZoomOut,
                    onAction = {
                        val newZoom = (zoom - step).coerceIn(minZoom, maxZoom)
                        onZoomChanged(newZoom)
                        if (newZoom == minZoom) onPanReset()
                    }
                )
                ZoomButton(
                    isZoomIn = true,
                    enabled = canZoomIn,
                    onAction = {
                        val newZoom = (zoom + step).coerceIn(minZoom, maxZoom)
                        onZoomChanged(newZoom)
                    }
                )
            }
        }
    }
}

@Composable
private fun ZoomButton(
    isZoomIn: Boolean,
    enabled: Boolean,
    onAction: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconRes = if (isZoomIn) {
        if (isPressed) R.drawable.ic_zoom_plus_on else R.drawable.ic_zoom_plus_off
    } else {
        if (isPressed) R.drawable.ic_zoom_moins_on else R.drawable.ic_zoom_moins_off
    }
    val contentDesc = if (isZoomIn) stringResource(R.string.act_zoom_plus) else stringResource(R.string.act_zoom_moins)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(AppDimens.cornerSmall))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) { onAction() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDesc,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun calculatePrecalculatedMap(
    geoJsonData: GeoJsonFeatureCollection,
    canvasWidth: Float,
    canvasHeight: Float,
    overseasTerritories: List<TerritoryBound>
): MapPathsData {
    val features = geoJsonData.features
    val latCorrection = cos(Math.toRadians(46.0))

    val mainlandPathsData = calculateMainlandPaths(features, canvasWidth, canvasHeight, latCorrection)
    val overseasPathsData = calculateOverseasPaths(features, canvasWidth, canvasHeight, overseasTerritories, latCorrection)

    return Pair(mainlandPathsData, overseasPathsData)
}

private fun calculateMainlandPaths(
    features: List<GeoJsonFeature>,
    canvasWidth: Float,
    canvasHeight: Float,
    latCorrection: Double
): List<Triple<Int, GeoJsonFeature, List<Path>>> {
    val mainlandLons = mutableListOf<Double>()
    val mainlandLats = mutableListOf<Double>()

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

    val mainlandPathsData = mutableListOf<Triple<Int, GeoJsonFeature, List<Path>>>()
    if (mainlandLons.isEmpty() || mainlandLats.isEmpty()) return mainlandPathsData

    val minLon = mainlandLons.minOrNull()!!
    val maxLon = mainlandLons.maxOrNull()!!
    val minLat = mainlandLats.minOrNull()!!
    val maxLat = mainlandLats.maxOrNull()!!

    val lonSpan = (maxLon - minLon) * latCorrection
    val latSpan = maxLat - minLat
    if (lonSpan <= 0 || latSpan <= 0) return mainlandPathsData

    val scaleX = canvasWidth / lonSpan
    val scaleY = canvasHeight / latSpan
    val scale = minOf(scaleX, scaleY) * 0.80f

    val mapWidth = lonSpan * scale
    val mapHeight = latSpan * scale
    val offsetXAdjustment = canvasWidth * 0.08f
    val offsetX = (canvasWidth - mapWidth) / 2f - (minLon * latCorrection) * scale + offsetXAdjustment
    val offsetY = (canvasHeight + mapHeight) / 2f + minLat * scale

    features.forEach { feature ->
        val crousId = feature.properties.crousId
        if (crousId != 8 && crousId != 29) {
            val paths = createPathsFromGeometry(
                feature.geometry.coordinates, scale, offsetX, offsetY, latCorrection
            )
            mainlandPathsData.add(Triple(crousId, feature, paths))
        }
    }
    return mainlandPathsData
}

private fun calculateOverseasPaths(
    features: List<GeoJsonFeature>,
    canvasWidth: Float,
    canvasHeight: Float,
    overseasTerritories: List<TerritoryBound>,
    latCorrection: Double
): List<Triple<Int, GeoJsonFeature, List<Path>>> {
    val vSize = canvasWidth * 0.08f
    val triangleCenterX = canvasWidth * 0.18f
    val triangleCenterY = canvasHeight * 0.46f
    val indianCenterX = canvasWidth * 0.24f
    val indianCenterY = canvasHeight * 0.88f

    val overseasConfigs = listOf(
        Triple("guyane", vSize, Offset(triangleCenterX - canvasWidth * 0.06f, triangleCenterY + canvasHeight * 0.16f)),
        Triple("guadeloupe", vSize, Offset(triangleCenterX - canvasWidth * 0.08f, triangleCenterY + canvasHeight * 0.06f)),
        Triple("martinique", vSize, Offset(triangleCenterX - canvasWidth * 0.00f, triangleCenterY + canvasHeight * 0.08f)),
        Triple("reunion", vSize, Offset(indianCenterX - canvasWidth * 0.02f, indianCenterY)),
        Triple("mayotte", vSize, Offset(indianCenterX - canvasWidth * 0.10f, indianCenterY - canvasHeight * 0.06f))
    )

    val overseasPathsData = mutableListOf<Triple<Int, GeoJsonFeature, List<Path>>>()

    overseasConfigs.forEach { (territoryKey, targetSize, targetCenter) ->
        val territory = overseasTerritories.find { it.key == territoryKey } ?: return@forEach
        val (west, south, east, north) = territory.bounds
        val isInsideBounds = { lng: Double, lat: Double -> lng in west..east && lat in south..north }

        features.forEach { feature ->
            val crousId = feature.properties.crousId
            val isTargetMatch = when (territoryKey) {
                "guyane", "guadeloupe", "martinique" -> crousId == 8
                "reunion", "mayotte" -> crousId == 29
                else -> false
            }

            if (isTargetMatch) {
                processTerritoryFeature(feature, isInsideBounds, targetSize, targetCenter, latCorrection, overseasPathsData)
            }
        }
    }
    return overseasPathsData
}

private fun processTerritoryFeature(
    feature: GeoJsonFeature,
    isInsideBounds: (Double, Double) -> Boolean,
    targetSize: Float,
    targetCenter: Offset,
    latCorrection: Double,
    outPathsData: MutableList<Triple<Int, GeoJsonFeature, List<Path>>>
) {
    val independentPolys = extractIndependentPolygons(feature.geometry.coordinates)
    val territoryPolygons = mutableListOf<List<List<JsonArray>>>()

    independentPolys.forEach { polyRings ->
        val firstCoord = polyRings.firstOrNull()?.firstOrNull()
        if (firstCoord != null && firstCoord.size >= 2) {
            try {
                val lng = firstCoord[0].jsonPrimitive.double
                val lat = firstCoord[1].jsonPrimitive.double
                if (isInsideBounds(lng, lat)) {
                    territoryPolygons.add(polyRings)
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    if (territoryPolygons.isNotEmpty()) {
        buildVignettePaths(feature, territoryPolygons, targetSize, targetCenter, latCorrection, outPathsData)
    }
}

private fun buildVignettePaths(
    feature: GeoJsonFeature,
    territoryPolygons: List<List<List<JsonArray>>>,
    targetSize: Float,
    targetCenter: Offset,
    latCorrection: Double,
    outPathsData: MutableList<Triple<Int, GeoJsonFeature, List<Path>>>
) {
    val tLons = mutableListOf<Double>()
    val tLats = mutableListOf<Double>()
    territoryPolygons.forEach { polyRings ->
        polyRings.forEach { ring ->
            ring.forEach { coord ->
                try {
                    tLons.add(coord[0].jsonPrimitive.double)
                    tLats.add(coord[1].jsonPrimitive.double)
                } catch (_: Exception) {
                    // Ignore
                }
            }
        }
    }

    if (tLons.isEmpty() || tLats.isEmpty()) return

    val tMinLon = tLons.minOrNull()!!
    val tMaxLon = tLons.maxOrNull()!!
    val tMinLat = tLats.minOrNull()!!
    val tMaxLat = tLats.maxOrNull()!!

    val tLonSpan = (tMaxLon - tMinLon) * latCorrection
    val tLatSpan = tMaxLat - tMinLat
    if (tLonSpan <= 0 || tLatSpan <= 0) return

    val localScale = minOf(targetSize / tLonSpan, targetSize / tLatSpan)
    val tOffsetX = -(tMinLon * latCorrection) * localScale
    val tOffsetY = tMaxLat * localScale

    val vignettePaths = mutableListOf<Path>()
    territoryPolygons.forEach { polyRings ->
        vignettePaths.addAll(createPathsFromRings(polyRings, localScale, tOffsetX, tOffsetY, latCorrection))
    }

    var minX = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var minY = Float.MAX_VALUE
    var maxY = Float.MIN_VALUE

    vignettePaths.forEach { path ->
        val bounds = path.getBounds()
        minX = minOf(minX, bounds.left)
        maxX = maxOf(maxX, bounds.right)
        minY = minOf(minY, bounds.top)
        maxY = maxOf(maxY, bounds.bottom)
    }

    if (minX < maxX && minY < maxY) {
        val deltaX = targetCenter.x - (minX + maxX) / 2f
        val deltaY = targetCenter.y - (minY + maxY) / 2f

        vignettePaths.forEach { path ->
            path.translate(Offset(deltaX, deltaY))
        }

        outPathsData.add(Triple(feature.properties.crousId, feature, vignettePaths))
    }
}