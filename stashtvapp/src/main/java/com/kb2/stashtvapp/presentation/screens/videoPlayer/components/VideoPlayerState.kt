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

package com.kb2.stashtvapp.presentation.screens.videoPlayer.components

import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi

@androidx.annotation.OptIn(UnstableApi::class)
class VideoPlayerState(
    @IntRange(from = 0)
    val hideSeconds: Int,
) {
    var isControlsVisible by mutableStateOf(true)
        private set

    var lastInteractionTime by mutableLongStateOf(0L)
        private set

    fun showControls() {
        isControlsVisible = true
        lastInteractionTime = System.currentTimeMillis()
    }

    fun hideControls() {
        isControlsVisible = false
    }
}

/**
 * Create and remember a [VideoPlayerState] instance. Useful when trying to control the state of
 * the [VideoPlayerOverlay]-related composable.
 * @return A remembered instance of [VideoPlayerState].
 * @param hideSeconds How many seconds should the controls be visible before being hidden.
 * */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun rememberVideoPlayerState(
    @IntRange(from = 0) hideSeconds: Int = 2
): VideoPlayerState {
    return remember {
        VideoPlayerState(
            hideSeconds = hideSeconds,
        )
    }
}
