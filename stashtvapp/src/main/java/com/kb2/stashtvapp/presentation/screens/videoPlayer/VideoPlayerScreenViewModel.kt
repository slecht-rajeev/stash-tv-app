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

package com.kb2.stashtvapp.presentation.screens.videoPlayer

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kb2.stashtvapp.data.entities.SceneDetails
import com.kb2.stashtvapp.data.repositories.SceneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.ConcurrentHashMap

@HiltViewModel
class VideoPlayerScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SceneRepository,
) : ViewModel() {
    private val recordedScenePlays = ConcurrentHashMap.newKeySet<String>()

    val uiState = savedStateHandle
        .getStateFlow<String?>(VideoPlayerScreen.SceneIdBundleKey, null)
        .map { id ->
            if (id == null) {
                VideoPlayerScreenUiState.Error
            } else {
                val details = repository.getSceneDetails(sceneId = id)
                VideoPlayerScreenUiState.Done(sceneDetails = details)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VideoPlayerScreenUiState.Loading
        )

    fun markScenePlayed(sceneId: String) {
        if (!recordedScenePlays.add(sceneId)) return

        viewModelScope.launch {
            runCatching {
                repository.recordScenePlay(sceneId)
            }.onFailure {
                recordedScenePlays.remove(sceneId)
                println("Failed to record play for scene $sceneId: ${it.javaClass.simpleName}: ${it.message}")
            }
        }
    }

    fun updateResumeTime(sceneId: String, resumeTime: Double) {
        viewModelScope.launch {
            runCatching {
                repository.saveSceneActivity(sceneId, resumeTime)
            }.onFailure {
                println("Failed to update resume time for scene $sceneId: ${it.javaClass.simpleName}: ${it.message}")
            }
        }
    }
}

@Immutable
sealed class VideoPlayerScreenUiState {
    data object Loading : VideoPlayerScreenUiState()
    data object Error : VideoPlayerScreenUiState()
    data class Done(val sceneDetails: SceneDetails) : VideoPlayerScreenUiState()
}
