package com.smirtom.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smirtom.app.data.CalendarRepository
import com.smirtom.app.data.CollectionDay
import com.smirtom.app.data.PreferencesManager
import com.smirtom.app.data.SyncState
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
    val syncState: SyncState = SyncState.Idle,
    val commune: String = "Magny-en-Vexin"
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
                _uiState.value = _uiState.value.copy(syncState = sync)
            }
        }
        refresh()
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            repository.ensureCalendarSynced(force = force)
            loadUpcoming()
        }
    }

    private suspend fun loadUpcoming() {
        val tomorrow = LocalDate.now(zoneId).plusDays(1)
        val tomorrowTypes = repository.getCollectionsOn(tomorrow)
        val upcoming = repository.getUpcomingEvents(limit = 6)

        _uiState.value = _uiState.value.copy(
            tomorrowLabel = tomorrow.format(dateFormatter).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString()
            },
            tomorrowWasteTypes = tomorrowTypes,
            upcoming = upcoming
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
    val reminderHour: StateFlow<Int> = preferencesManager.reminderHour.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PreferencesManager.DEFAULT_REMINDER_HOUR
    )

    fun setReminderHour(hour: Int) {
        viewModelScope.launch {
            preferencesManager.setReminderHour(hour)
            repository.rescheduleReminders(hour)
        }
    }

    fun refreshCalendar() {
        viewModelScope.launch {
            repository.ensureCalendarSynced(force = true)
        }
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
