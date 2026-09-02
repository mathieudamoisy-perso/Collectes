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

data class HomeSnapshot(
    val tomorrowLabel: String,
    val tomorrowWasteTypes: List<WasteType>,
    val upcoming: List<CollectionDay>,
    val communeSlug: String
)

class CalendarRepository(
    private val context: Context,
    private val fetcher: SmirtomFetcher = SmirtomFetcher(),
    private val parser: PdfCalendarParser = PdfCalendarParser(),
    private val encombrantsFetcher: EncombrantsFetcher = EncombrantsFetcher(),
    private val communeRulesFetcher: CommuneRulesFetcher = CommuneRulesFetcher()
) {
    private val database = AppDatabase.get(context)
    private val collectionDao = database.collectionDao()
    private val syncMetadataDao = database.syncMetadataDao()
    private val reminderScheduler = ReminderScheduler(context)
    private val preferencesManager = PreferencesManager(context)
    private val zoneId = ZoneId.of("Europe/Paris")

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    companion object {
        @Volatile
        private var cachedHomeSnapshot: HomeSnapshot? = null

        fun peekHomeSnapshot(): HomeSnapshot? = cachedHomeSnapshot

        internal fun updateCachedHomeSnapshot(snapshot: HomeSnapshot?) {
            cachedHomeSnapshot = snapshot
        }
    }

    fun peekHomeSnapshot(): HomeSnapshot? = Companion.peekHomeSnapshot()

    suspend fun warmUpHomeSnapshot() = withContext(Dispatchers.IO) {
        updateCachedHomeSnapshot(buildHomeSnapshot(filter = null))
    }

    suspend fun refreshHomeSnapshotCache(filter: WasteType? = null) = withContext(Dispatchers.IO) {
        updateCachedHomeSnapshot(buildHomeSnapshot(filter))
    }

    private suspend fun buildHomeSnapshot(filter: WasteType?): HomeSnapshot? {
        if (!isCachedCalendarForSelectedCommune()) return null
        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)
        val commune = preferencesManager.getSelectedCommune()
        val tomorrowTypes = getCollectionsOn(tomorrow, filter = null)
        val upcoming = getUpcomingEvents(filter)
        val tomorrowLabel = formatDateLabel(tomorrow)
        return HomeSnapshot(
            tomorrowLabel = tomorrowLabel,
            tomorrowWasteTypes = tomorrowTypes,
            upcoming = upcoming,
            communeSlug = commune.slug
        )
    }

    private fun formatDateLabel(date: LocalDate): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", java.util.Locale.FRENCH)
        return date.format(formatter).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(java.util.Locale.FRENCH) else it.toString()
        }
    }

    suspend fun ensureCalendarSynced(force: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            PDFBoxResourceLoader.init(context.applicationContext)

            val commune = preferencesManager.getSelectedCommune()
            val currentYear = LocalDate.now(zoneId).year
            if (force) {
                clearLocalCalendarCache()
                _syncState.value = SyncState.Loading
            }
            val metadata = syncMetadataDao.get()
            val logicOutdated =
                preferencesManager.getCalendarLogicVersion() < PreferencesManager.CALENDAR_LOGIC_VERSION
            if (!force &&
                !logicOutdated &&
                metadata?.calendarYear == currentYear &&
                metadata.communeSlug == commune.slug
            ) {
                val cached = collectionDao.getEventsFrom(LocalDate.now(zoneId).toEpochDay())
                if (cached.isNotEmpty()) {
                    reminderScheduler.scheduleUpcomingReminders(
                        cached.mapNotNull { it.toCollectionDay() },
                        preferencesManager.getReminderTimeMinutes()
                    )
                    _syncState.value = SyncState.Success(
                        Instant.ofEpochMilli(metadata.lastSyncEpochMillis),
                        metadata.calendarYear
                    )
                    return@runCatching metadata.calendarYear
                }
            }

            _syncState.value = SyncState.Loading

            val pdfUrl = commune.officialCalendarUrl.takeIf { it.isNotBlank() }
                ?: runCatching { fetcher.findPdfUrl(currentYear, commune) }.getOrNull()
            val pdfText = if (pdfUrl != null) {
                runCatching {
                    val pdfFile = fetcher.downloadPdf(
                        pdfUrl,
                        fetcher.pdfCacheFile(context.filesDir, currentYear, commune.slug)
                    )
                    parser.extractText(pdfFile)
                }.getOrNull()
            } else {
                null
            }
            val pageText = runCatching {
                communeRulesFetcher.fetchText(commune)
            }.getOrNull()

            val rules = CalendarReconciler.reconcile(
                pdfText = pdfText,
                pageText = pageText,
                commune = commune,
                year = currentYear
            )
            val regularEvents = CalendarDateGenerator.generate(
                currentYear,
                rules,
                includeNextYearJanuary = true
            )

            val encombrantsEvents = if (rules.encombrantsMonthOrdinal != null) {
                CalendarDateGenerator.encombrantsDates(currentYear, rules, includeNextYearJanuary = true)
                    .map { date -> CollectionDay(date, listOf(WasteType.ENCOMBRANTS)) }
            } else {
                encombrantsFetcher.toCollectionDays(
                    encombrantsFetcher.fetchDates(currentYear, commune)
                )
            }
            val events = CollectionDayMerger.merge(regularEvents + encombrantsEvents)

            collectionDao.clearAll()
            collectionDao.insertAll(events.map { it.toEntity() })
            syncMetadataDao.upsert(
                SyncMetadataEntity(
                    calendarYear = currentYear,
                    lastSyncEpochMillis = System.currentTimeMillis(),
                    pdfUrl = pdfUrl,
                    communeSlug = commune.slug
                )
            )

            reminderScheduler.scheduleUpcomingReminders(
                events,
                preferencesManager.getReminderTimeMinutes()
            )
            preferencesManager.setCalendarLogicVersion(PreferencesManager.CALENDAR_LOGIC_VERSION)

            _syncState.value = SyncState.Success(Instant.now(), currentYear)
            currentYear
        }.onFailure { error ->
            _syncState.value = SyncState.Error(error.message ?: "Erreur inconnue")
        }
    }

    suspend fun getSelectedCommune(): VexinCommune = preferencesManager.getSelectedCommune()

    suspend fun setCommune(commune: VexinCommune) {
        preferencesManager.setCommune(commune)
        updateCachedHomeSnapshot(null)
        clearLocalCalendarCache()
    }

    suspend fun clearLocalCalendarCache() = withContext(Dispatchers.IO) {
        collectionDao.clearAll()
        syncMetadataDao.clear()
        updateCachedHomeSnapshot(null)
        fetcher.deleteCachedPdfs(context.filesDir)
        fetcher.deleteCachedPdfs(context.cacheDir)
    }

    suspend fun getUpcomingEvents(filter: WasteType? = null): List<CollectionDay> = withContext(Dispatchers.IO) {
        if (!isCachedCalendarForSelectedCommune()) return@withContext emptyList()
        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)
        val events = collectionDao.getEventsFrom(today.toEpochDay())
            .mapNotNull { it.toCollectionDay() }
            .filter { it.date.isAfter(tomorrow) }
            .sortedBy { it.date }
        applyFilter(events, filter)
    }

    suspend fun getCollectionsOn(date: LocalDate, filter: WasteType? = null): List<WasteType> = withContext(Dispatchers.IO) {
        if (!isCachedCalendarForSelectedCommune()) return@withContext emptyList()
        val types = collectionDao.getEventOn(date.toEpochDay())?.toCollectionDay()?.wasteTypes.orEmpty()
        if (filter == null) types else types.filter { it == filter }
    }

    private fun applyFilter(events: List<CollectionDay>, filter: WasteType?): List<CollectionDay> {
        if (filter == null) return events
        return events.mapNotNull { day ->
            if (filter !in day.wasteTypes) return@mapNotNull null
            day.copy(wasteTypes = listOf(filter))
        }
    }

    private suspend fun isCachedCalendarForSelectedCommune(): Boolean {
        val commune = preferencesManager.getSelectedCommune()
        val metadata = syncMetadataDao.get() ?: return false
        val currentYear = LocalDate.now(zoneId).year
        return metadata.communeSlug == commune.slug && metadata.calendarYear == currentYear
    }

    suspend fun getTomorrowCollections(filter: WasteType? = null): List<WasteType> {
        val tomorrow = LocalDate.now(zoneId).plusDays(1)
        return getCollectionsOn(tomorrow, filter)
    }

    suspend fun rescheduleReminders(reminderTimeMinutes: Int) = withContext(Dispatchers.IO) {
        val events = collectionDao.getEventsFrom(LocalDate.now(zoneId).toEpochDay())
            .mapNotNull { it.toCollectionDay() }
        reminderScheduler.scheduleUpcomingReminders(events, reminderTimeMinutes)
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
