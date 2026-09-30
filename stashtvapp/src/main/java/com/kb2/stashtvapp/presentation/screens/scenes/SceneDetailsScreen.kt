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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import com.kb2.stashtvapp.R
import com.kb2.stashtvapp.data.entities.Scene
import com.kb2.stashtvapp.data.entities.SceneDetails
import com.kb2.stashtvapp.data.util.StringConstants
import com.kb2.stashtvapp.presentation.common.Error
import com.kb2.stashtvapp.presentation.common.Loading
import com.kb2.stashtvapp.presentation.common.ScenesRow
import com.kb2.stashtvapp.presentation.screens.dashboard.rememberChildPadding

object SceneDetailsScreen {
    const val SceneIdBundleKey = "sceneId"
}

@Composable
fun SceneDetailsScreen(
    goToScenePlayer: () -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewScene: (Scene) -> Unit,
    sceneDetailsScreenViewModel: SceneDetailsScreenViewModel = hiltViewModel()
) {
    val uiState by sceneDetailsScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is SceneDetailsScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize())
        }

        is SceneDetailsScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize())
        }

        is SceneDetailsScreenUiState.Done -> {
            Details(
                sceneDetails = s.sceneDetails,
                goToScenePlayer = goToScenePlayer,
                onBackPressed = onBackPressed,
                refreshScreenWithNewScene = refreshScreenWithNewScene,
                onToggleFavorite = { sceneDetailsScreenViewModel.addFavorite(it) },
                onAddWatchLater = { sceneDetailsScreenViewModel.addWatchLater(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize()
            )
        }
    }
}

@Composable
private fun Details(
    sceneDetails: SceneDetails,
    goToScenePlayer: () -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewScene: (Scene) -> Unit,
    onToggleFavorite: (sceneId: String) -> Unit = {},
    onAddWatchLater: (sceneId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()

    BackHandler(onBack = onBackPressed)
    LazyColumn(
        contentPadding = PaddingValues(bottom = 135.dp),
        modifier = modifier,
    ) {
        item {
            SceneDetails(
                sceneDetails = sceneDetails,
                goToScenePlayer = goToScenePlayer,
                onToggleFavorite = onToggleFavorite,
                onAddWatchLater = onAddWatchLater
            )
        }

        item {
            CastAndCrewList(
                castAndCrew = sceneDetails.castAndCrew
            )
        }

        item {
            ScenesRow(
                title = StringConstants
                    .Composable
                    .sceneDetailsScreenSimilarTo(sceneDetails.name),
                titleStyle = MaterialTheme.typography.titleMedium,
                sceneList = sceneDetails.similarScenes,
                onSceneSelected = refreshScreenWithNewScene
            )
        }

        item {
            SceneReviews(
                modifier = Modifier.padding(top = childPadding.top),
                reviewsAndRatings = sceneDetails.reviewsAndRatings
            )
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = childPadding.start)
                    .padding(BottomDividerPadding)
                    .fillMaxWidth()
                    .height(1.dp)
                    .alpha(0.15f)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = childPadding.start),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val itemModifier = Modifier.width(192.dp)

                TitleValueText(
                    modifier = itemModifier,
                    title = stringResource(R.string.status),
                    value = sceneDetails.status
                )
                TitleValueText(
                    modifier = itemModifier,
                    title = stringResource(R.string.original_language),
                    value = sceneDetails.originalLanguage
                )
                TitleValueText(
                    modifier = itemModifier,
                    title = stringResource(R.string.budget),
                    value = sceneDetails.budget
                )
                TitleValueText(
                    modifier = itemModifier,
                    title = stringResource(R.string.revenue),
                    value = sceneDetails.revenue
                )
            }
        }
    }
}

private val BottomDividerPadding = PaddingValues(vertical = 48.dp)
