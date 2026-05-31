package fr.croustillapp.features.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "favorites")

class FavoriteManager(private val context: Context) {
    private val favoriKey = stringPreferencesKey("favorite_ids_json")

    val favoriteIds: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[favoriKey] ?: "[]"
            Json.decodeFromString<List<String>>(json).toSet()
        }

    suspend fun saveFavorites(ids: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[favoriKey] = Json.encodeToString(ids.toList())
        }
    }
}