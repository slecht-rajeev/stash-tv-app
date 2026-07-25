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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kb2.stashtvapp.data.entities.Scene
import com.kb2.stashtvapp.presentation.common.Loading
import com.kb2.stashtvapp.presentation.common.PosterImage
import com.kb2.stashtvapp.presentation.common.SceneCard
import com.kb2.stashtvapp.presentation.screens.dashboard.rememberChildPadding
import com.kb2.stashtvapp.presentation.screens.favourites.SceneFilterChip
import com.kb2.stashtvapp.presentation.theme.StashAppBottomListPadding

@Composable
fun ScenesScreen(
    onSceneClick: (scene: Scene) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    scenesScreenViewModel: ScenesScreenViewModel = hiltViewModel(),
) {
    val uiState by scenesScreenViewModel.uiState.collectAsStateWithLifecycle()
    
    when (val s = uiState) {
        is ScenesScreenUiState.Loading -> Loading(modifier = Modifier.fillMaxSize())
        is ScenesScreenUiState.Ready -> {
            Catalog(
                sceneList = s.sceneList,
                filterMode = s.filterMode,
                onFilterChange = scenesScreenViewModel::setFilterMode,
                onLoadMore = scenesScreenViewModel::loadMoreScenes,
                onSceneClick = onSceneClick,
                onScroll = onScroll,
                isTopBarVisible = isTopBarVisible,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Catalog(
    sceneList: List<Scene>,
    filterMode: SceneFilterMode,
    onFilterChange: (SceneFilterMode) -> Unit,
    onLoadMore: () -> Unit,
    onSceneClick: (scene: Scene) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()
    val gridState = rememberLazyGridState()
    
    val shouldShowTopBar by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 &&
                gridState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(shouldShowTopBar) {
        onScroll(shouldShowTopBar)
    }
    
    LaunchedEffect(isTopBarVisible) {
        if (isTopBarVisible) gridState.animateScrollToItem(0)
    }

    // Pagination logic
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= sceneList.size - 10 // Load more when 10 items away from end
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    Column(modifier = modifier.padding(horizontal = childPadding.start)) {
        // Filter Chips Row
        Row(
            modifier = Modifier.padding(top = childPadding.top, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SceneFilterChip(
                label = "All",
                isChecked = filterMode == SceneFilterMode.ALL,
                onCheckedChange = { if (it) onFilterChange(SceneFilterMode.ALL) }
            )
            SceneFilterChip(
                label = "Favourites",
                isChecked = filterMode == SceneFilterMode.FAVOURITES,
                onCheckedChange = { if (it) onFilterChange(SceneFilterMode.FAVOURITES) }
            )
            SceneFilterChip(
                label = "Watch Later",
                isChecked = filterMode == SceneFilterMode.WATCH_LATER,
                onCheckedChange = { if (it) onFilterChange(SceneFilterMode.WATCH_LATER) }
            )
        }

        // Paginated Randomized Grid
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = StashAppBottomListPadding)
        ) {
            itemsIndexed(sceneList, key = { index, scene -> "${scene.id}_$index" }) { _, scene ->
                SceneCard(
                    onClick = { onSceneClick(scene) },
                    modifier = Modifier.aspectRatio(16 / 9f)
                ) {
                    PosterImage(scene = scene, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
