package com.collectes.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database(
    entities = [CollectionEventEntity::class, SyncMetadataEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        private const val DB_NAME = "collectes.db"
        private const val LEGACY_DB_NAME = "smirtom.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: run {
                    migrateLegacyDatabaseIfNeeded(context.applicationContext)
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DB_NAME
                    )
                        .fallbackToDestructiveMigration()
                        .build().also { instance = it }
                }
            }
        }

        private fun migrateLegacyDatabaseIfNeeded(context: Context) {
            val newDb = context.getDatabasePath(DB_NAME)
            if (newDb.exists()) return
            val legacyDb = context.getDatabasePath(LEGACY_DB_NAME)
            if (!legacyDb.exists()) return
            newDb.parentFile?.mkdirs()
            for (suffix in listOf("", "-shm", "-wal")) {
                val from = File(legacyDb.path + suffix)
                if (!from.exists()) continue
                val to = File(newDb.path + suffix)
                if (!from.renameTo(to)) {
                    from.copyTo(to, overwrite = true)
                    from.delete()
                }
            }
        }
    }
}
