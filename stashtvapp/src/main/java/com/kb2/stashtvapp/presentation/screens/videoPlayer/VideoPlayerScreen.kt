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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import com.kb2.stashtvapp.data.entities.SceneDetails
import com.kb2.stashtvapp.presentation.common.Error
import com.kb2.stashtvapp.presentation.common.Loading
import com.kb2.stashtvapp.presentation.screens.videoPlayer.components.VideoPlayerControls
import com.kb2.stashtvapp.presentation.screens.videoPlayer.components.VideoPlayerOverlay
import com.kb2.stashtvapp.presentation.screens.videoPlayer.components.VideoPlayerState
import com.kb2.stashtvapp.presentation.screens.videoPlayer.components.rememberPlayer
import com.kb2.stashtvapp.presentation.screens.videoPlayer.components.rememberVideoPlayerState
import com.kb2.stashtvapp.presentation.utils.handleDPadRepeatKeyEvents
import kotlinx.coroutines.delay

object VideoPlayerScreen {
    const val SceneIdBundleKey = "sceneId"
}

@Composable
fun VideoPlayerScreen(
    onBackPressed: () -> Unit,
    videoPlayerScreenViewModel: VideoPlayerScreenViewModel = hiltViewModel()
) {
    val uiState by videoPlayerScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is VideoPlayerScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize())
        }

        is VideoPlayerScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize())
        }

        is VideoPlayerScreenUiState.Done -> {
            LaunchedEffect(s.sceneDetails.id) {
                videoPlayerScreenViewModel.markScenePlayed(s.sceneDetails.id)
            }

            VideoPlayerScreenContent(
                sceneDetails = s.sceneDetails,
                onBackPressed = onBackPressed,
                updateResumeTime = { videoPlayerScreenViewModel.updateResumeTime(s.sceneDetails.id, it) }
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreenContent(
    sceneDetails: SceneDetails,
    onBackPressed: () -> Unit,
    updateResumeTime: (Double) -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = rememberPlayer(context)

    val videoPlayerState = rememberVideoPlayerState(
        hideSeconds = 2,
    )

     LaunchedEffect(exoPlayer, sceneDetails) {
        exoPlayer.setMediaItem(MediaItem.fromUri(sceneDetails.videoUri))
        if (sceneDetails.resumeTime > 0) {
            exoPlayer.seekTo(sceneDetails.resumeTime.toLong() * 1000)
        }
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Periodic resume time update (every 1 minute)
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(60_000)
            if (exoPlayer.isPlaying) {
                updateResumeTime(exoPlayer.currentPosition.toDouble() / 1000)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                exoPlayer.stop()
                exoPlayer.release()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    BackHandler(onBack = onBackPressed)

    Box(
        Modifier
            .fillMaxSize()
            .dPadEvents(exoPlayer, videoPlayerState)
            .focusable()
    ) {
        PlayerSurface(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = Modifier.resizeWithContentScale(
                contentScale = ContentScale.Fit,
                sourceSizeDp = null
            )
        )

        val focusRequester = remember { FocusRequester() }
        VideoPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            isPlaying = exoPlayer.isPlaying,
            isControlsVisible = videoPlayerState.isControlsVisible,
            showControls = videoPlayerState::showControls,
            controls = {
                VideoPlayerControls(
                    player = exoPlayer,
                    sceneDetails = sceneDetails,
                    focusRequester = focusRequester,
                    onShowControls = { videoPlayerState.showControls(exoPlayer.isPlaying) },
                )
            }
        )
    }
}

private fun Modifier.dPadEvents(
    exoPlayer: ExoPlayer,
    videoPlayerState: VideoPlayerState
): Modifier = this.handleDPadRepeatKeyEvents(
    onLeft = {
        exoPlayer.seekBack()
        videoPlayerState.showControls(exoPlayer.isPlaying)
    },
    onRight = {
        exoPlayer.seekForward()
        videoPlayerState.showControls(exoPlayer.isPlaying)
    },
    onUp = { videoPlayerState.showControls() },
    onDown = { videoPlayerState.showControls() },
    onEnter = {
        if (!videoPlayerState.isControlsVisible) {
            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            videoPlayerState.showControls(exoPlayer.isPlaying)
        }
    }
)
