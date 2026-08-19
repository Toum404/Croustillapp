package fr.croustillapp.features.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// FR: Extension deleguee initialisant une instance unique de DataStore par cycle de vie d'application.
// EN: Singleton delegate extension initializing a single DataStore lifecycle instance across the app.
private val Context.dataStore by preferencesDataStore(name = "favorites")

/**
 * FR: Gestionnaire de persistance pour sauvegarder et observer la liste des restaurants favoris de l'utilisateur.
 * EN: Persistence manager designed to save and observe the user's favorite restaurant identifiers.
 */
class FavoriteManager(private val context: Context) {
    // FR: Cle d'indexation unique pour le stockage de la chaine JSON dans le magasin de preferences.
    // EN: Unique indexation preference key targeting the raw JSON string payload within the data store.
    private val favoriteKey = stringPreferencesKey("favorite_ids_json")

    /**
     * FR: Flux asynchrone (Flow) observant les identifiants favoris, deserialises depuis une structure JSON.
     * EN: Asynchronous cold stream (Flow) observing favorite identifiers, deserialized from a JSON structure.
     */
    val favoriteIds: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[favoriteKey] ?: "[]"
            // FR: Conversion en Set pour garantir l'unicite et optimiser le temps de recherche en O(1).
            // EN: Decoded into a Set structure to enforce unique constraints and optimize lookup queries to O(1).
            Json.decodeFromString<List<String>>(json).toSet()
        }

    /**
     * FR: Persiste de façon asynchrone la collection d'identifiants sous forme de tableau JSON serialise.
     * EN: Asynchronously commits the active identifiers collection down into a serialized JSON array block.
     */
    suspend fun saveFavorites(ids: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[favoriteKey] = Json.encodeToString(ids.toList())
        }
    }
}