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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kb2.stashtvapp.data.entities.Scene
import com.kb2.stashtvapp.data.entities.SceneList
import com.kb2.stashtvapp.data.repositories.SceneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SceneFilterMode {
    ALL, FAVOURITES, WATCH_LATER
}

@HiltViewModel
class ScenesScreenViewModel @Inject constructor(
    private val sceneRepository: SceneRepository
) : ViewModel() {

    private val _filterMode = MutableStateFlow(SceneFilterMode.ALL)
    val filterMode = _filterMode.asStateFlow()

    private val _scenes = MutableStateFlow<List<Scene>>(emptyList())
    private var currentPage = 1
    private var isLastPage = false
    private var isLoadingMore = false

    val uiState: StateFlow<ScenesScreenUiState> = combine(
        _scenes,
        _filterMode
    ) { scenes, mode ->
        if (scenes.isEmpty() && currentPage == 1) {
             ScenesScreenUiState.Loading
        } else {
            ScenesScreenUiState.Ready(
                sceneList = scenes,
                filterMode = mode
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScenesScreenUiState.Loading
    )

    init {
        loadScenes()
    }

    fun setFilterMode(mode: SceneFilterMode) {
        if (_filterMode.value == mode) return
        _filterMode.value = mode
        resetPagination()
        loadScenes()
    }

    fun loadMoreScenes() {
        if (isLoadingMore || isLastPage) return
        currentPage++
        loadScenes()
    }

    private fun resetPagination() {
        currentPage = 1
        isLastPage = false
        _scenes.value = emptyList()
    }

    private fun loadScenes() {
        isLoadingMore = true
        viewModelScope.launch {
            val flow = when (_filterMode.value) {
                SceneFilterMode.ALL -> sceneRepository.getScenesPaginated(currentPage, 30, randomized = true)
                SceneFilterMode.FAVOURITES -> sceneRepository.getFavouriteScenesPaginated(currentPage, 30)
                SceneFilterMode.WATCH_LATER -> sceneRepository.getWatchLaterScenesPaginated(currentPage, 30)
            }
            
            flow.collect { newScenes ->
                if (newScenes.isEmpty()) {
                    isLastPage = true
                } else {
                    _scenes.update { currentList -> currentList + newScenes }
                }
                isLoadingMore = false
            }
        }
    }
}

sealed interface ScenesScreenUiState {
    data object Loading : ScenesScreenUiState
    data class Ready(
        val sceneList: List<Scene>,
        val filterMode: SceneFilterMode
    ) : ScenesScreenUiState
}
