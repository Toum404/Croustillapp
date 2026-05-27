package fr.croustillapp.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class SingleRestaurantResponse(
    val success: Boolean,
    val data: RestaurantDto
)

@Dao
interface RestaurantDao {

    @Query("SELECT * FROM restaurants")
    fun getAllRestaurants(): Flow<List<RestaurantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(restaurants: List<RestaurantEntity>)

    @Update(entity = RestaurantEntity::class)
    suspend fun updateAllStatuses(statusUpdates: List<StatusUpdatePartial>)

    @Query("DELETE FROM restaurants")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM restaurants")
    suspend fun getRestaurantsCount(): Int
}

data class StatusUpdatePartial(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "isOpen") val isOpen: Boolean
)