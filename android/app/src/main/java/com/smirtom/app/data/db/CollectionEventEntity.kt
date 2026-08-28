package com.smirtom.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "collection_events")
data class CollectionEventEntity(
    @PrimaryKey val dateEpochDay: Long,
    val wasteTypes: String
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val id: Int = 1,
    val calendarYear: Int,
    val lastSyncEpochMillis: Long,
    val pdfUrl: String?
)

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collection_events WHERE dateEpochDay >= :fromEpochDay ORDER BY dateEpochDay ASC")
    suspend fun getEventsFrom(fromEpochDay: Long): List<CollectionEventEntity>

    @Query("SELECT * FROM collection_events WHERE dateEpochDay = :epochDay LIMIT 1")
    suspend fun getEventOn(epochDay: Long): CollectionEventEntity?

    @Query("DELETE FROM collection_events")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<CollectionEventEntity>)
}

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE id = 1")
    suspend fun get(): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)
}
