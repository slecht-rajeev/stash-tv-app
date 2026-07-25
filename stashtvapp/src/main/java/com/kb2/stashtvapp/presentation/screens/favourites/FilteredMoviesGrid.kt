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

package com.kb2.stashtvapp.presentation.screens.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kb2.stashtvapp.data.entities.SceneList
import com.kb2.stashtvapp.presentation.common.SceneCard
import com.kb2.stashtvapp.presentation.common.PosterImage
import com.kb2.stashtvapp.presentation.theme.StashAppBottomListPadding

@Composable
fun FilteredScenesGrid(
    state: LazyGridState,
    sceneList: SceneList,
    onSceneClick: (sceneId: String) -> Unit,
) {
    LazyVerticalGrid(
        state = state,
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(6),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = StashAppBottomListPadding),
    ) {
        items(sceneList, key = { it.id }) { scene ->
            SceneCard(
                onClick = { onSceneClick(scene.id) },
                modifier = Modifier.aspectRatio(1 / 1.5f),
            ) {
                PosterImage(scene = scene, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
