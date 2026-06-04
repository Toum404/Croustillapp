package fr.croustillapp.features.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * FR: Convertisseurs Room permettant de sérialiser/désérialiser les types complexes en String JSON dans SQLite.
 * EN: Room TypeConverters mapping complex embedded data types into plain JSON Strings within SQLite columns.
 */
class DataConverters {

    companion object {
        // FR: Configuration de Json pour ignorer les clés inconnues en cas d'évolution future de l'API.
        // EN: Json configuration setup to safely ignore unknown keys during structural API updates.
        private val json = Json { ignoreUnknownKeys = true }
    }

    /**
     * FR: Convertit une liste de chaînes de caractères en texte JSON pour le stockage.
     * EN: Serializes a list of primitive string tokens into a persistent JSON block.
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    /**
     * FR: Reconstitue la liste de chaînes de caractères depuis le texte JSON SQLite.
     * EN: Deserializes a persistent JSON block back into a standard string list structure.
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { json.decodeFromString(it) }
    }

    /**
     * FR: Sérialise la structure complexe des jours d'ouverture (DTO) en texte JSON.
     * EN: Serializes structural operational calendar models (DTO list) into a JSON text string.
     */
    @TypeConverter
    fun fromJoursOuvertList(value: List<JourOuvertDto>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    /**
     * FR: Restitue la structure complexe des jours d'ouverture (DTO) depuis le texte JSON SQLite.
     * EN: Restores structural operational calendar models (DTO list) from a persistent JSON database record.
     */
    @TypeConverter
    fun toJoursOuvertList(value: String?): List<JourOuvertDto>? {
        return value?.let { json.decodeFromString(it) }
    }
}