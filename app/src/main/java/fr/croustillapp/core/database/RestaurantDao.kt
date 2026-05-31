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
 * Interface d'accès aux données (DAO) pour exécuter des requêtes SQL sur la table "restaurants".
 * Data Access Object (DAO) providing structured SQLite entry points for the "restaurants" table.
 */
@Dao
interface RestaurantDao {

    /**
     * Récupère un flux continu (Flow) de tous les restaurants triés localement.
     * Observes a continuous data stream monitoring the entire local restaurants catalog.
     */
    @Query("SELECT * FROM restaurants")
    fun getAllRestaurants(): Flow<List<RestaurantEntity>>

    /**
     * Insère une liste de restaurants. Écrase les anciennes données en cas de conflit d'ID.
     * Persists multiple restaurant rows. Overwrites overlapping entries on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(restaurants: List<RestaurantEntity>)

    /**
     * Met à jour uniquement des champs partiels ciblés (comme le statut d'ouverture) pour économiser des ressources.
     * Performs lightweight target row updates focusing exclusively on specific partial property updates.
     */
    @Update(entity = RestaurantEntity::class)
    suspend fun updateAllStatuses(statusUpdates: List<StatusUpdatePartial>)

    /**
     * Vide complètement la table des restaurants (utile pour forcer un rafraîchissement complet).
     * Clears all entries from the local repository table.
     */
    @Query("DELETE FROM restaurants")
    suspend fun deleteAll()

    /**
     * Compte le nombre total d'entrées en cache. Utile pour savoir si la base est vide au démarrage.
     * Checks existing cache depth by counting total rows stored inside the table.
     */
    @Query("SELECT COUNT(*) FROM restaurants")
    suspend fun getRestaurantsCount(): Int
}

/**
 * Modèle partiel utilisé par Room pour mettre à jour efficacement le statut d'ouverture sans réécrire toute la ligne.
 * Lightweight partial data transfer object used by Room to patch rows without re-writing full entities.
 */
data class StatusUpdatePartial(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "isOpen") val isOpen: Boolean
)