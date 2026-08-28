package com.smirtom.app.data

import android.content.Context
import com.smirtom.app.data.db.AppDatabase
import com.smirtom.app.data.db.CollectionEventEntity
import com.smirtom.app.data.db.SyncMetadataEntity
import com.smirtom.app.notifications.ReminderScheduler
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed class SyncState {
    data object Idle : SyncState()
    data object Loading : SyncState()
    data class Success(val lastSync: Instant, val year: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

class CalendarRepository(
    private val context: Context,
    private val fetcher: SmirtomFetcher = SmirtomFetcher(),
    private val parser: PdfCalendarParser = PdfCalendarParser()
) {
    private val database = AppDatabase.get(context)
    private val collectionDao = database.collectionDao()
    private val syncMetadataDao = database.syncMetadataDao()
    private val reminderScheduler = ReminderScheduler(context)
    private val preferencesManager = PreferencesManager(context)
    private val zoneId = ZoneId.of("Europe/Paris")

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun ensureCalendarSynced(force: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            _syncState.value = SyncState.Loading
            PDFBoxResourceLoader.init(context.applicationContext)

            val currentYear = LocalDate.now(zoneId).year
            val metadata = syncMetadataDao.get()
            if (!force && metadata?.calendarYear == currentYear) {
                val cached = collectionDao.getEventsFrom(LocalDate.now(zoneId).toEpochDay())
                if (cached.isNotEmpty()) {
                    reminderScheduler.scheduleUpcomingReminders(
                        cached.mapNotNull { it.toCollectionDay() },
                        preferencesManager.getReminderHour()
                    )
                    _syncState.value = SyncState.Success(
                        Instant.ofEpochMilli(metadata.lastSyncEpochMillis),
                        metadata.calendarYear
                    )
                    return@runCatching metadata.calendarYear
                }
            }

            val pdfUrl = fetcher.findPdfUrl(currentYear)
            val pdfFile = fetcher.downloadPdf(pdfUrl, fetcher.pdfCacheFile(context.filesDir, currentYear))
            val events = parser.parse(pdfFile, currentYear)

            collectionDao.clearAll()
            collectionDao.insertAll(events.map { it.toEntity() })
            syncMetadataDao.upsert(
                SyncMetadataEntity(
                    calendarYear = currentYear,
                    lastSyncEpochMillis = System.currentTimeMillis(),
                    pdfUrl = pdfUrl
                )
            )

            reminderScheduler.scheduleUpcomingReminders(
                events,
                preferencesManager.getReminderHour()
            )

            _syncState.value = SyncState.Success(Instant.now(), currentYear)
            currentYear
        }.onFailure { error ->
            _syncState.value = SyncState.Error(error.message ?: "Erreur inconnue")
        }
    }

    suspend fun getUpcomingEvents(limit: Int = 8): List<CollectionDay> = withContext(Dispatchers.IO) {
        collectionDao.getEventsFrom(LocalDate.now(zoneId).toEpochDay())
            .mapNotNull { it.toCollectionDay() }
            .take(limit)
    }

    suspend fun getCollectionsOn(date: LocalDate): List<WasteType> = withContext(Dispatchers.IO) {
        collectionDao.getEventOn(date.toEpochDay())?.toCollectionDay()?.wasteTypes.orEmpty()
    }

    suspend fun getTomorrowCollections(): List<WasteType> {
        val tomorrow = LocalDate.now(zoneId).plusDays(1)
        return getCollectionsOn(tomorrow)
    }

    suspend fun rescheduleReminders(reminderHour: Int) = withContext(Dispatchers.IO) {
        val events = collectionDao.getEventsFrom(LocalDate.now(zoneId).toEpochDay())
            .mapNotNull { it.toCollectionDay() }
        reminderScheduler.scheduleUpcomingReminders(events, reminderHour)
    }

    private fun CollectionDay.toEntity(): CollectionEventEntity {
        return CollectionEventEntity(
            dateEpochDay = date.toEpochDay(),
            wasteTypes = wasteTypes.joinToString(",") { it.name }
        )
    }

    private fun CollectionEventEntity.toCollectionDay(): CollectionDay? {
        val types = wasteTypes.split(",")
            .mapNotNull { WasteType.fromStorage(it.trim()) }
        if (types.isEmpty()) return null
        return CollectionDay(LocalDate.ofEpochDay(dateEpochDay), types)
    }
}
