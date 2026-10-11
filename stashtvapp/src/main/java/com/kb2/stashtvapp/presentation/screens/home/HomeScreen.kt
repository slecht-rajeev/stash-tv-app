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

package com.kb2.stashtvapp.presentation.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.kb2.stashtvapp.data.entities.SceneList
import com.kb2.stashtvapp.data.entities.HomeSection
import com.kb2.stashtvapp.presentation.common.Error
import com.kb2.stashtvapp.presentation.common.Loading
import com.kb2.stashtvapp.presentation.common.ScenesRow
import com.kb2.stashtvapp.presentation.screens.dashboard.rememberChildPadding

@Composable
fun HomeScreen(
    onSceneClick: (scene: Scene) -> Unit,
    onCategoryClick: (categoryId: String) -> Unit = {},
    goToVideoPlayer: (scene: Scene) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    homeScreeViewModel: HomeScreeViewModel = hiltViewModel(),
) {
    val uiState by homeScreeViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is HomeScreenUiState.Ready -> {
            Catalog(
                featuredScenes = s.featuredSceneList,
                watchLaterScenes = s.watchLaterScenes,
                homeSections = s.homeSections,
                positionsSubTags = s.positionsSubTags,
                onSceneClick = onSceneClick,
                onCategoryClick = onCategoryClick,
                onScroll = onScroll,
                goToVideoPlayer = goToVideoPlayer,
                onToggleFavorite = { homeScreeViewModel.addFavorite(it) },
                onAddWatchLater = { homeScreeViewModel.addWatchLater(it) },
                isTopBarVisible = isTopBarVisible,
                modifier = Modifier.fillMaxSize(),
            )
        }

        is HomeScreenUiState.Loading -> Loading(modifier = Modifier.fillMaxSize())
        is HomeScreenUiState.Error -> Error(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun Catalog(
    featuredScenes: SceneList,
    watchLaterScenes: SceneList,
    homeSections: List<HomeSection>,
    positionsSubTags: List<com.kb2.stashtvapp.data.entities.SceneCategory> = emptyList(),
    onSceneClick: (scene: Scene) -> Unit,
    onCategoryClick: (categoryId: String) -> Unit = {},
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    goToVideoPlayer: (scene: Scene) -> Unit,
    onToggleFavorite: (scene: Scene) -> Unit = {},
    onAddWatchLater: (scene: Scene) -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    isTopBarVisible: Boolean = true,
) {

    val lazyListState = rememberLazyListState()
    val childPadding = rememberChildPadding()

    val shouldShowTopBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 &&
                lazyListState.firstVisibleItemScrollOffset < 300
        }
    }

    LaunchedEffect(shouldShowTopBar) {
        onScroll(shouldShowTopBar)
    }
    LaunchedEffect(isTopBarVisible) {
        if (isTopBarVisible) lazyListState.animateScrollToItem(0)
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(bottom = 108.dp),
        // Setting overscan margin to bottom to ensure the last row's visibility
        modifier = modifier,
    ) {

        item(contentType = "FeaturedScenesCarousel") {
            StackedHeroCarousel(
                items = featuredScenes,
                modifier = Modifier.padding(
                    start = 48.dp,
                    top = 24.dp,
                    end = 24.dp
                ),
                onSceneClick = { scene ->
                    goToVideoPlayer(scene)
                }
            )
        }

        if (positionsSubTags.isNotEmpty()) {
            item(contentType = "PositionsSection") {
                PositionsSection(
                    subTags = positionsSubTags,
                    onSubTagClick = onCategoryClick,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

//        if (watchLaterScenes.isNotEmpty()) {
//            item(contentType = "WatchLaterImmersiveList") {
//                WatchLaterImmersiveList(
//                    sceneList = watchLaterScenes,
//                    onSceneClick = onSceneClick,
//                    modifier = Modifier.padding(top = 16.dp)
//                )
//            }
//        }

        homeSections.forEach { section ->
            item(contentType = "ScenesRow") {
//                ScenesRow(
//                    modifier = Modifier.padding(top = 16.dp),
//                    sceneList = section.scenes,
//                    title = section.title,
//                    onSceneSelected = onSceneClick
//                )
                WatchLaterImmersiveList(
                    sceneList = section.scenes,
                    onSceneClick = onSceneClick,
                    title = section.title,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}