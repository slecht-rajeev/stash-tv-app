package com.kb2.stashtvapp.presentation.screens.studios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kb2.stashtvapp.data.entities.StudioDetails
import com.kb2.stashtvapp.data.repositories.SceneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StudioDetailsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sceneRepository: SceneRepository
) : ViewModel() {

    val uiState =
        savedStateHandle.getStateFlow<String?>(
            StudioDetailsScreen.StudioIdBundleKey,
            null
        ).map { id ->
            if (id == null) {
                StudioDetailsScreenUiState.Error
            } else {
                val studioDetails = sceneRepository.getStudioDetails(id)
                StudioDetailsScreenUiState.Done(studioDetails)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudioDetailsScreenUiState.Loading
        )
}

sealed interface StudioDetailsScreenUiState {
    data object Loading : StudioDetailsScreenUiState
    data object Error : StudioDetailsScreenUiState
    data class Done(val studioDetails: StudioDetails) : StudioDetailsScreenUiState
}
