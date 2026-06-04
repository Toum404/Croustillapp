package fr.croustillapp.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.croustillapp.features.data.DataConverters
import fr.croustillapp.features.data.RestaurantEntity

// FR: Déclaration de la base de données Room avec ses entités, sa version et la désactivation de l'export du schéma.
// EN: Room database declaration specifying entities, version number, and disabling schema export.
@Database(entities = [RestaurantEntity::class], version = 3, exportSchema = false)
@TypeConverters(DataConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun restaurantDao(): RestaurantDao

    companion object {
        // FR: Garantie que les modifications de INSTANCE sont immédiatement visibles par tous les threads.
        // EN: Ensures that updates to INSTANCE are immediately visible across all execution threads.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // FR: Pattern Singleton : renvoie l'instance existante ou la crée de manière synchronisée si elle n'existe pas.
            // EN: Singleton pattern: returns the existing instance or creates it synchronously if it does not exist yet.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "croustillapp_database"
                )
                    // FR: Reconstruit la base de données en cas de changement de version sans migration explicite (à manipuler prudemment en production).
                    // EN: Rebuilds the database during version increments without explicit migration paths (handle with care in production).
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}