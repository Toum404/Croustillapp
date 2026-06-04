package fr.croustillapp.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import fr.croustillapp.features.data.RestaurantEntity
import kotlinx.coroutines.flow.Flow

/**
 * FR: Interface d'accès aux données (DAO) pour gérer les opérations SQL liées aux restaurants.
 * EN: Data Access Object (DAO) interface defining SQL operations for restaurant entities.
 */
@Dao
interface RestaurantDao {

    // FR: Récupère tous les restaurants sous forme de Flow pour une mise à jour réactive de l'UI.
    // EN: Retrieves all restaurants as a Flow stream to ensure reactive UI updates.
    @Query("SELECT * FROM restaurants")
    fun getAllRestaurants(): Flow<List<RestaurantEntity>>

    // FR: Insère une liste de restaurants et remplace les doublons existants (via l'ID).
    // EN: Inserts a list of restaurants, replacing any conflicting duplicates based on primary keys.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(restaurants: List<RestaurantEntity>)

    // FR: Met à jour partiellement les entités pour rafraîchir uniquement les statuts d'ouverture.
    // EN: Performs a partial entity update targeting exclusively the opening status fields.
    @Update(entity = RestaurantEntity::class)
    suspend fun updateAllStatuses(statusUpdates: List<StatusUpdatePartial>)

    // FR: Vide intégralement la table des restaurants (utilisé pour rafraîchir le cache local).
    // EN: Clears the entire restaurants table (typically used during local cache invalidation).
    @Query("DELETE FROM restaurants")
    suspend fun deleteAll()

    // FR: Compte le nombre total de restaurants actuellement stockés dans la base locale.
    // EN: Counts the total number of restaurant records currently stored in the local cache.
    @Query("SELECT COUNT(*) FROM restaurants")
    suspend fun getRestaurantsCount(): Int
}

/**
 * FR: Classe de données partielle optimisant les requêtes de mise à jour des horaires d'ouverture.
 * EN: Partial data class optimized for targeting specific layout updates regarding business hours.
 */
data class StatusUpdatePartial(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "isOpen") val isOpen: Boolean
)