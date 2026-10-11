/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kb2.stashtvapp.presentation.screens.scenes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kb2.stashtvapp.data.entities.SceneCategory
import com.kb2.stashtvapp.data.entities.SceneDetails
import com.kb2.stashtvapp.data.repositories.SceneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SceneDetailsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SceneRepository,
) : ViewModel() {

    private val sceneId: String? = savedStateHandle[SceneDetailsScreen.SceneIdBundleKey]

    private val _positionSubTags = MutableStateFlow<List<SceneCategory>>(emptyList())
    val positionSubTags: StateFlow<List<SceneCategory>> = _positionSubTags.asStateFlow()

    private val _tagUpdateError = MutableStateFlow<String?>(null)
    val tagUpdateError: StateFlow<String?> = _tagUpdateError.asStateFlow()

    private val _sceneDetails = MutableStateFlow<SceneDetails?>(null)

    val uiState: StateFlow<SceneDetailsScreenUiState> = combine(
        _sceneDetails,
        _tagUpdateError
    ) { details, error ->
        when {
            details != null -> SceneDetailsScreenUiState.Done(sceneDetails = details, errorMessage = error)
            sceneId == null -> SceneDetailsScreenUiState.Error
            else -> SceneDetailsScreenUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SceneDetailsScreenUiState.Loading
    )

    private var isUpdatingTags = false

    init {
        if (sceneId != null) {
            loadSceneDetails(sceneId)
            loadPositionSubTags()
        }
    }

    private fun loadSceneDetails(id: String) {
        viewModelScope.launch {
            try {
                val details = repository.getSceneDetails(sceneId = id)
                _sceneDetails.value = details
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun loadPositionSubTags() {
        viewModelScope.launch {
            repository.getPositionsSubTags().collect { tags ->
                _positionSubTags.value = tags
            }
        }
    }

    fun addPositionTag(tagId: String) {
        val currentDetails = _sceneDetails.value ?: return
        if (isUpdatingTags) return

        val positionTags = _positionSubTags.value
        val targetPosition = positionTags.find { it.id == tagId } ?: return

        // Prevent duplicate assignment
        if (currentDetails.tags.any { it.id == tagId }) return

        val previousTags = currentDetails.tags
        val updatedTags = previousTags + targetPosition

        // Optimistic UI update
        _sceneDetails.value = currentDetails.copy(
            tags = updatedTags,
            categories = updatedTags.map { it.name }
        )
        _tagUpdateError.value = null

        val newTagIds = updatedTags.map { it.id }

        viewModelScope.launch {
            isUpdatingTags = true
            val success = repository.updateSceneTags(currentDetails.id, newTagIds)
            if (!success) {
                // Rollback on failure
                _sceneDetails.value = currentDetails.copy(
                    tags = previousTags,
                    categories = previousTags.map { it.name }
                )
                _tagUpdateError.value = "Failed to add position tag. Rolled back."
            }
            isUpdatingTags = false
        }
    }

    fun removePositionTag(tagId: String) {
        val currentDetails = _sceneDetails.value ?: return
        if (isUpdatingTags) return

        val previousTags = currentDetails.tags
        if (previousTags.none { it.id == tagId }) return

        val updatedTags = previousTags.filter { it.id != tagId }

        // Optimistic UI update
        _sceneDetails.value = currentDetails.copy(
            tags = updatedTags,
            categories = updatedTags.map { it.name }
        )
        _tagUpdateError.value = null

        val newTagIds = updatedTags.map { it.id }

        viewModelScope.launch {
            isUpdatingTags = true
            val success = repository.updateSceneTags(currentDetails.id, newTagIds)
            if (!success) {
                // Rollback on failure
                _sceneDetails.value = currentDetails.copy(
                    tags = previousTags,
                    categories = previousTags.map { it.name }
                )
                _tagUpdateError.value = "Failed to remove position tag. Rolled back."
            }
            isUpdatingTags = false
        }
    }

    fun addFavorite(sceneId: String) {
        viewModelScope.launch {
            repository.addFavorite(sceneId)
        }
    }

    fun addWatchLater(sceneId: String) {
        viewModelScope.launch {
            repository.addWatchLaterTag(sceneId)
        }
    }
}

sealed interface SceneDetailsScreenUiState {
    data object Loading : SceneDetailsScreenUiState
    data object Error : SceneDetailsScreenUiState
    data class Done(
        val sceneDetails: SceneDetails,
        val errorMessage: String? = null
    ) : SceneDetailsScreenUiState
}