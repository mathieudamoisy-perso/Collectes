package com.smirtom.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smirtom.app.data.CalendarRepository
import com.smirtom.app.data.CollectionDay
import com.smirtom.app.data.PreferencesManager
import com.smirtom.app.data.ReminderTime
import com.smirtom.app.data.SyncState
import com.smirtom.app.data.VexinCommune
import com.smirtom.app.data.VexinCommunes
import com.smirtom.app.data.WasteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val commune: String = "Magny-en-Vexin",
    val contentCommuneSlug: String? = null,
    val isLoadingNewCommune: Boolean = false
)

class HomeViewModel(
    private val repository: CalendarRepository
) : ViewModel() {
    private val zoneId = ZoneId.of("Europe/Paris")
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
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
                                isLoadingNewCommune = true
                            )
                        }
                    }
                    is SyncState.Success -> {
                        _uiState.value = _uiState.value.copy(syncState = sync)
                        loadUpcoming(_uiState.value.activeFilter)
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(syncState = sync)
                    }
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
            _uiState.value = _uiState.value.copy(activeFilter = filter)
            loadUpcoming(filter)
        }
    }

    private suspend fun loadUpcoming(filter: WasteType?) {
        val today = LocalDate.now(zoneId)
        val commune = repository.getSelectedCommune()
        val tomorrow = today.plusDays(1)
        val tomorrowTypes = repository.getCollectionsOn(tomorrow, filter)
        val filteredUpcoming = repository.getUpcomingEvents(filter = filter)

        _uiState.value = _uiState.value.copy(
            tomorrowLabel = tomorrow.format(dateFormatter).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
            },
            tomorrowWasteTypes = tomorrowTypes,
            upcoming = filteredUpcoming,
            activeFilter = filter,
            commune = commune.displayName,
            contentCommuneSlug = commune.slug,
            isLoadingNewCommune = false
        )
    }
}

class HomeViewModelFactory(
    private val repository: CalendarRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repository) as T
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
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VexinCommunes.default
    )

    val communes: List<VexinCommune> = VexinCommunes.all

    private val _calendarError = MutableStateFlow<String?>(null)
    val calendarError: StateFlow<String?> = _calendarError.asStateFlow()

    fun setReminderTime(minutesOfDay: Int) {
        viewModelScope.launch {
            preferencesManager.setReminderTime(minutesOfDay)
            repository.rescheduleReminders(preferencesManager.getReminderTimeMinutes())
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
