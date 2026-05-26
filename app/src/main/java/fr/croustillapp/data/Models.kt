package fr.croustillapp.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "restaurants")
data class RestaurantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String?,
    val acceptsIzly: Boolean,
    val pmr: Boolean,
    val isOpen: Boolean,
    val region: String,
    val type: String,
    val adresse: String?,
    val latitude: Double,
    val longitude: Double,
    val email: String?,
    val telephone: String?,
    val horaires: String?
)

@Immutable
data class Restaurant(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val acceptsIzly: Boolean,
    val pmr: Boolean = true,
    val isOpen: Boolean = true,
    val region: String,
    val type: String,
    val adresse: String?,
    val latitude: Double,
    val longitude: Double,
    val email: String?,
    val telephone: String?,
    val horaires: List<String>? = null
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val data: List<RestaurantDto>
)

@Serializable
data class RestaurantDto(
    val code: Int,
    val region: RegionDto,
    val type: TypeDto,
    val nom: String,
    val adresse: String? = null,
    val latitude: Double,
    val longitude: Double,
    val horaires: List<String>? = null,
    @SerialName("jours_ouvert")
    val joursOuvert: List<JourOuvertDto>,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val email: String? = null,
    val telephone: String? = null,
    @SerialName("ispmr")
    val isPmr: Boolean? = null,
    val zone: String,
    val paiement: Set<String>? = null,
    val acces: List<String>? = null,
    val ouvert: Boolean,
    val actif: Boolean? = null
)

@Serializable
data class RegionDto(val code: Int, val libelle: String)

@Serializable
data class TypeDto(val code: Int, val libelle: String)

@Serializable
data class JourOuvertDto(val jour: String, val ouverture: OuvertureDto)

@Serializable
data class OuvertureDto(val matin: Boolean, val midi: Boolean, val soir: Boolean)

val MyJsonParser = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun RestaurantDto.toEntity(): RestaurantEntity {
    return RestaurantEntity(
        id = code.toString(),
        name = nom,
        imageUrl = imageUrl,
        acceptsIzly = paiement?.contains("IZLY") == true,
        pmr = isPmr ?: false,
        isOpen = ouvert,
        region = region.libelle,
        type = type.libelle,
        adresse = adresse,
        latitude = latitude,
        longitude = longitude,
        email = email,
        telephone = telephone,
        horaires = horaires?.let { MyJsonParser.encodeToString(it) }
    )
}

fun RestaurantEntity.toDomain(): Restaurant {
    return Restaurant(
        id = id,
        name = name,
        imageUrl = imageUrl,
        acceptsIzly = acceptsIzly,
        pmr = pmr,
        isOpen = isOpen,
        region = region,
        type = type,
        adresse = adresse,
        latitude = latitude,
        longitude = longitude,
        email = email,
        telephone = telephone,
        horaires = horaires?.let { MyJsonParser.decodeFromString<List<String>>(it) }
    )
}

@Serializable
data class MenuResponse(
    val success: Boolean,
    val data: List<DailyMenuDto>
)

@Serializable
data class DailyMenuDto(
    val date: String,
    val repas: List<RepasDto>
)

@Serializable
data class RepasDto(
    val type: String,
    val categories: List<CategoryDto>
)

@Serializable
data class CategoryDto(
    val libelle: String,
    val plats: List<PlatDto>
)

@Serializable
data class PlatDto(
    val libelle: String
)

@Serializable
data class RestaurantStatusMinimalResponse(
    val success: Boolean,
    val data: List<RestaurantStatusMinimalDto>
)

@Serializable
data class RestaurantStatusMinimalDto(
    val code: Int,
    val ouvert: Boolean,
    val actif: Boolean
)