package fr.croustillapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GeoJsonFeatureCollection(
    val type: String,
    val features: List<GeoJsonFeature>
)

@Serializable
data class GeoJsonFeature(
    val type: String,
    val properties: CrousProperties,
    val geometry: GeoJsonGeometry
)

@Serializable
data class CrousProperties(
    @SerialName("crous_id")
    val crousId: Int,

    @SerialName("crous_nom")
    val crousNom: String
)

@Serializable
data class GeoJsonGeometry(
    val type: String,
    val coordinates: JsonElement
)