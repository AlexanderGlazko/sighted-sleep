package com.sighted.sleep.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sighted.sleep.SightedSleepApp
import com.sighted.sleep.ui.edit.EditSessionViewModel
import com.sighted.sleep.ui.history.HistoryViewModel
import com.sighted.sleep.ui.today.TodayViewModel

object AppViewModels {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { TodayViewModel(app().sleepRepository) }
        initializer { HistoryViewModel(app().sleepRepository) }
        initializer { EditSessionViewModel(app().sleepRepository, createSavedStateHandle()) }
    }
}

private fun androidx.lifecycle.viewmodel.CreationExtras.app(): SightedSleepApp =
    this[APPLICATION_KEY] as SightedSleepApp
