package com.sighted.sleep.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sighted.sleep.SightedSleepApp
import com.sighted.sleep.ui.edit.EditSessionViewModel
import com.sighted.sleep.ui.history.HistoryViewModel
import com.sighted.sleep.ui.settings.SettingsViewModel
import com.sighted.sleep.ui.today.TodayViewModel
import com.sighted.sleep.ui.timeline.TimelineViewModel
import com.sighted.sleep.ui.insights.InsightsViewModel

object AppViewModels {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { app().run { TodayViewModel(sleepRepository, tagRepository, settingsRepository) } }
        initializer { app().run { HistoryViewModel(sleepRepository, tagRepository, settingsRepository) } }
        initializer {
            app().run { EditSessionViewModel(sleepRepository, tagRepository, settingsRepository, createSavedStateHandle()) }
        }
        initializer { app().run { SettingsViewModel(settingsRepository, tagRepository, sleepRepository, backupRepository) } }
        initializer { app().run { TimelineViewModel(sleepRepository, settingsRepository) } }
        initializer { app().run { InsightsViewModel(sleepRepository, tagRepository, settingsRepository) } }
    }
}

private fun CreationExtras.app(): SightedSleepApp = this[APPLICATION_KEY] as SightedSleepApp
