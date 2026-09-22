package com.collectes.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.collectes.app.data.CalendarRepository
import com.collectes.app.data.CollectionDay
import com.collectes.app.data.PreferencesManager
import com.collectes.app.data.ReminderTime
import com.collectes.app.data.SyncState
import com.collectes.app.data.VexinCommune
import com.collectes.app.data.VexinCommunes
import com.collectes.app.data.WasteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HomeUiState(
    val tomorrowLabel: String = "",
    val tomorrowWasteTypes: List<WasteType> = emptyList(),
    val upcoming: List<CollectionDay> = emptyList(),
    val activeFilter: WasteType? = null,
    val syncState: SyncState = SyncState.Idle,
    val commune: String = "",
    val contentCommuneSlug: String? = null,
    val isLoadingNewCommune: Boolean = false,
    val isInitialLoading: Boolean = true
)

class HomeViewModel(
    private val repository: CalendarRepository,
    private val preferencesManager: PreferencesManager,
    initialCommune: VexinCommune
) : ViewModel() {
    private val zoneId = ZoneId.of("Europe/Paris")
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

    private val _uiState = MutableStateFlow(buildInitialState(initialCommune))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private fun buildInitialState(commune: VexinCommune): HomeUiState {
        val snapshot = repository.peekHomeSnapshot()
        return if (snapshot != null && snapshot.communeSlug == commune.slug) {
            HomeUiState(
                tomorrowLabel = snapshot.tomorrowLabel,
                tomorrowWasteTypes = snapshot.tomorrowWasteTypes,
                upcoming = snapshot.upcoming,
                commune = commune.displayName,
                contentCommuneSlug = commune.slug,
                syncState = SyncState.Success(snapshot.lastSync, snapshot.calendarYear),
                isInitialLoading = false
            )
        } else {
            HomeUiState(
                commune = commune.displayName,
                contentCommuneSlug = commune.slug,
                isInitialLoading = true
            )
        }
    }

    init {
        viewModelScope.launch {
            loadUpcoming(_uiState.value.activeFilter)
        }
        viewModelScope.launch {
            preferencesManager.selectedCommune.collect { commune ->
                if (commune.slug != _uiState.value.contentCommuneSlug) {
                    _uiState.value = _uiState.value.copy(
                        commune = commune.displayName,
                        tomorrowWasteTypes = emptyList(),
                        upcoming = emptyList(),
                        activeFilter = null,
                        isLoadingNewCommune = true
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.syncState.collect { sync ->
                when (sync) {
                    is SyncState.Loading -> {
                        val commune = repository.getSelectedCommune()
                        val sameCommune = commune.slug == _uiState.value.contentCommuneSlug
                        _uiState.value = if (sameCommune) {
                            _uiState.value.copy(
                                syncState = sync,
                                commune = commune.displayName,
                                isLoadingNewCommune = false
                            )
                        } else {
                            _uiState.value.copy(
                                syncState = sync,
                                commune = commune.displayName,
                                tomorrowWasteTypes = emptyList(),
                                upcoming = emptyList(),
                                activeFilter = null,
                                isLoadingNewCommune = true
                            )
                        }
                    }
                    is SyncState.Success -> {
                        _uiState.value = _uiState.value.copy(syncState = sync)
                        loadUpcoming(_uiState.value.activeFilter)
                    }
                    is SyncState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            syncState = sync,
                            isLoadingNewCommune = false,
                            isInitialLoading = false
                        )
                    }
                    // Idle : ne pas effacer une Success déjà lue depuis le cache local
                    is SyncState.Idle -> Unit
                }
            }
        }
        refresh()
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            repository.ensureCalendarSynced(force = force)
            loadUpcoming(_uiState.value.activeFilter)
        }
    }

    /** Recomputes tomorrow / upcoming when the app returns to the foreground. */
    fun reloadDates() {
        viewModelScope.launch {
            loadUpcoming(_uiState.value.activeFilter)
        }
    }

    fun setFilter(filter: WasteType?) {
        viewModelScope.launch {
            val filteredUpcoming = repository.getUpcomingEvents(filter = filter)
            _uiState.value = _uiState.value.copy(
                activeFilter = filter,
                upcoming = filteredUpcoming
            )
            repository.refreshHomeSnapshotCache(filter)
        }
    }

    private suspend fun loadUpcoming(filter: WasteType?) {
        val today = LocalDate.now(zoneId)
        val commune = repository.getSelectedCommune()
        val tomorrow = today.plusDays(1)
        val tomorrowTypes = repository.getCollectionsOn(tomorrow, filter = null)
        val filteredUpcoming = repository.getUpcomingEvents(filter = filter)
        val cachedSync = repository.getCachedSyncSuccess()
        val currentSync = _uiState.value.syncState
        val nextSync = when (currentSync) {
            is SyncState.Loading, is SyncState.Error -> currentSync
            is SyncState.Success -> currentSync
            is SyncState.Idle -> cachedSync ?: currentSync
        }

        _uiState.value = _uiState.value.copy(
            tomorrowLabel = tomorrow.format(dateFormatter).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
            },
            tomorrowWasteTypes = tomorrowTypes,
            upcoming = filteredUpcoming,
            activeFilter = filter,
            commune = commune.displayName,
            contentCommuneSlug = commune.slug,
            syncState = nextSync,
            isLoadingNewCommune = false,
            isInitialLoading = !repository.hasCachedCalendar() &&
                nextSync !is SyncState.Error
        )
        repository.refreshHomeSnapshotCache(filter)
    }

    suspend fun findNextCollection(type: WasteType): LocalDate? {
        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)
        val tomorrowTypes = repository.getCollectionsOn(tomorrow, type)
        if (tomorrowTypes.isNotEmpty()) return tomorrow
        return repository.getUpcomingEvents(filter = type).firstOrNull()?.date
    }
}

class HomeViewModelFactory(
    private val repository: CalendarRepository,
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(
            repository,
            preferencesManager,
            preferencesManager.peekSelectedCommune()
        ) as T
    }
}

class SettingsViewModel(
    private val repository: CalendarRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    val reminderTimeMinutes: StateFlow<Int> = preferencesManager.reminderTimeMinutes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReminderTime.DEFAULT_MINUTES
    )

    val selectedCommune: StateFlow<VexinCommune> = preferencesManager.selectedCommune.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = preferencesManager.peekSelectedCommune()
    )

    val useBrandColors: StateFlow<Boolean> = preferencesManager.useBrandColors.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    val enabledReminderTypes: StateFlow<Set<WasteType>> =
        preferencesManager.enabledReminderTypes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WasteType.entries.toSet()
        )

    private val _availableReminderTypes = MutableStateFlow(WasteType.entries.toList())
    val availableReminderTypes: StateFlow<List<WasteType>> = _availableReminderTypes.asStateFlow()

    val communes: List<VexinCommune> = VexinCommunes.all

    private val _calendarError = MutableStateFlow<String?>(null)
    val calendarError: StateFlow<String?> = _calendarError.asStateFlow()

    val syncState: StateFlow<SyncState> = repository.syncState

    init {
        viewModelScope.launch {
            combine(selectedCommune, syncState) { commune, sync -> commune.slug to sync }
                .collectLatest {
                    _availableReminderTypes.value = repository.getCollectedWasteTypes()
                }
        }
    }

    fun setReminderTime(minutesOfDay: Int) {
        viewModelScope.launch {
            preferencesManager.setReminderTime(minutesOfDay)
            repository.rescheduleReminders(preferencesManager.getReminderTimeMinutes())
        }
    }

    fun setReminderTypeEnabled(type: WasteType, enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setReminderTypeEnabled(type, enabled)
            repository.rescheduleReminders(preferencesManager.getReminderTimeMinutes())
        }
    }

    fun setUseBrandColors(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setUseBrandColors(enabled)
        }
    }

    fun setCommune(commune: VexinCommune) {
        viewModelScope.launch {
            repository.setCommune(commune)
            repository.ensureCalendarSynced(force = true)
        }
    }

    fun officialCalendarViewUrl(): String = selectedCommune.value.officialCalendarUrl

    fun reportCalendarOpenError() {
        _calendarError.value = "Impossible d'ouvrir le navigateur"
    }
}

class SettingsViewModelFactory(
    private val repository: CalendarRepository,
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(repository, preferencesManager) as T
    }
}
