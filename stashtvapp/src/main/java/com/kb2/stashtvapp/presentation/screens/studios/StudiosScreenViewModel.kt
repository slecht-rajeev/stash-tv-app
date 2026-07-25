package com.kb2.stashtvapp.presentation.screens.studios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kb2.stashtvapp.data.entities.Studio
import com.kb2.stashtvapp.data.repositories.SceneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StudiosScreenViewModel @Inject constructor(
    sceneRepository: SceneRepository
) : ViewModel() {

    val uiState = sceneRepository.getStudios().map {
        StudiosScreenUiState.Ready(studioList = it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudiosScreenUiState.Loading
    )
}

sealed interface StudiosScreenUiState {
    data object Loading : StudiosScreenUiState
    data class Ready(val studioList: List<Studio>) : StudiosScreenUiState
}
