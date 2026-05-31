package fr.croustillapp.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import fr.croustillapp.features.data.RestaurantEntity

/**
 * Point d'accès principal pour la base de données relationnelle locale Room de l'application.
 * Main abstract architectural entry point hosting the local relational Room database.
 */
@Database(entities = [RestaurantEntity::class], version = 2, exportSchema = false) // -------------- Version 2 : "zone"
abstract class AppDatabase : RoomDatabase() {

    /**
     * Fournit le DAO pour interagir avec la table des restaurants.
     * Provides the Data Access Object interface for restaurant table operations.
     */
    abstract fun restaurantDao(): RestaurantDao

    companion object {
        // L'annotation @Volatile garantit que les modifications de l'instance sont immédiatement visibles par tous les threads
        // @Volatile ensures atomic variable mutations remain instantly visible across active thread boundaries
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Récupère l'instance unique (Singleton) de la base de données. Initialisation thread-safe.
         * Retrieves the unique database thread-safe Singleton instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "croustillapp_database"
                )
                    // Stratégie de repli en cas de changement de version sans script de migration (recréation brute)
                    // Recreates database tables forcefully if schema updates lack explicit migration paths
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}