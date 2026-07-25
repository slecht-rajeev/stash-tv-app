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

package com.kb2.stashtvapp.presentation.screens.search

import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kb2.stashtvapp.R
import com.kb2.stashtvapp.data.entities.Scene
import com.kb2.stashtvapp.data.entities.SearchResult
import com.kb2.stashtvapp.data.entities.SceneCast
import com.kb2.stashtvapp.data.entities.SceneCategory
import com.kb2.stashtvapp.data.entities.SceneGroup
import com.kb2.stashtvapp.data.entities.SceneMarker
import com.kb2.stashtvapp.presentation.common.ScenesRow
import com.kb2.stashtvapp.presentation.screens.dashboard.rememberChildPadding
import com.kb2.stashtvapp.presentation.theme.StashAppCardShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CompactCard
import androidx.tv.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.WideClassicCard
import coil.compose.AsyncImage

@Composable
fun SearchScreen(
    onSceneClick: (scene: Scene) -> Unit,
    onTagClick: (tagId: String) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    searchScreenViewModel: SearchScreenViewModel = hiltViewModel(),
) {
    val lazyColumnState = rememberLazyListState()
    val shouldShowTopBar by remember {
        derivedStateOf {
            lazyColumnState.firstVisibleItemIndex == 0 &&
                lazyColumnState.firstVisibleItemScrollOffset < 100
        }
    }

    val searchState by searchScreenViewModel.searchState.collectAsStateWithLifecycle()

    LaunchedEffect(shouldShowTopBar) {
        onScroll(shouldShowTopBar)
    }

    when (val s = searchState) {
        is SearchState.Searching -> {
            Text(text = "Searching...")
        }

        is SearchState.Done -> {
            SearchResult(
                searchResult = s.searchResult,
                expandedCategory = s.expandedCategory,
                searchScenes = searchScreenViewModel::query,
                onSceneClick = onSceneClick,
                onTagClick = onTagClick,
                onViewAllClick = { category -> searchScreenViewModel.fetchAll(category) },
                onBackFromExpansion = { searchScreenViewModel.resetExpansion() },
                modifier = Modifier.fillMaxSize()
            )
        }
        is SearchState.Error -> {
            Text(text = "Error: ${s.message}")
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchResult(
    searchResult: SearchResult,
    expandedCategory: String?,
    searchScenes: (queryString: String) -> Unit,
    onSceneClick: (scene: Scene) -> Unit,
    onTagClick: (tagId: String) -> Unit,
    onViewAllClick: (category: String) -> Unit,
    onBackFromExpansion: () -> Unit,
    modifier: Modifier = Modifier,
    lazyColumnState: LazyListState = rememberLazyListState(),
) {
    val childPadding = rememberChildPadding()
    var searchQuery by remember { mutableStateOf("") }
    val tfFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val tfInteractionSource = remember { MutableInteractionSource() }

    val isTfFocused by tfInteractionSource.collectIsFocusedAsState()

    if (expandedCategory != null) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = childPadding.start)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                Button(onClick = onBackFromExpansion) {
                    Text("Back")
                }
                Spacer(Modifier.width(16.dp))
                Text(text = "All $expandedCategory", style = MaterialTheme.typography.headlineMedium)
            }
            
            LazyColumn(modifier = Modifier.weight(1f).padding(top = 16.dp)) {
                when (expandedCategory) {
                    "Scenes" -> {
                        items(searchResult.scenes.chunked(4)) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                                row.forEach { scene ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        SceneItem(scene, onSceneClick)
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    "Performers" -> {
                        items(searchResult.performers.chunked(6)) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { performer ->
                                    Box(modifier = Modifier.weight(1f)) { PerformerItem(performer) }
                                }
                                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    "Tags" -> {
                        items(searchResult.tags.chunked(4)) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                                row.forEach { tag ->
                                    Box(modifier = Modifier.weight(1f)) { TagItem(tag, onTagClick) }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    "Groups" -> {
                        items(searchResult.groups.chunked(3)) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { group ->
                                    Box(modifier = Modifier.weight(1f)) { GroupItem(group) }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    "Markers" -> {
                        items(searchResult.markers.chunked(3)) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { marker ->
                                    Box(modifier = Modifier.weight(1f)) { MarkerItem(marker) }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        state = lazyColumnState
    ) {
        item {
            Surface(
                shape = ClickableSurfaceDefaults.shape(shape = StashAppCardShape),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.inverseOnSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                    pressedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
                    focusedContentColor = MaterialTheme.colorScheme.onSurface,
                    pressedContentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(
                            width = if (isTfFocused) 2.dp else 1.dp,
                            color = animateColorAsState(
                                targetValue = if (isTfFocused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.border,
                                label = ""
                            ).value
                        ),
                        shape = StashAppCardShape
                    )
                ),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .padding(horizontal = childPadding.start)
                    .padding(top = 8.dp),
                onClick = { tfFocusRequester.requestFocus() }
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { updatedQuery -> searchQuery = updatedQuery },
                    decorationBox = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .padding(start = 20.dp),
                        ) {
                            it()
                            if (searchQuery.isEmpty()) {
                                Text(
                                    modifier = Modifier.graphicsLayer { alpha = 0.6f },
                                    text = stringResource(R.string.search_screen_et_placeholder),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 4.dp,
                            horizontal = 8.dp
                        )
                        .focusRequester(tfFocusRequester)
                        .onKeyEvent {
                            if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                when (it.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        focusManager.moveFocus(FocusDirection.Down)
                                        return@onKeyEvent true
                                    }

                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        focusManager.moveFocus(FocusDirection.Up)
                                        return@onKeyEvent true
                                    }

                                    KeyEvent.KEYCODE_BACK -> {
                                        focusManager.moveFocus(FocusDirection.Exit)
                                        return@onKeyEvent true
                                    }
                                }
                            }
                            false
                        },
                    cursorBrush = Brush.verticalGradient(
                        colors = listOf(
                            LocalContentColor.current,
                            LocalContentColor.current,
                        )
                    ),
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            searchScenes(searchQuery)
                        }
                    ),
                    maxLines = 1,
                    interactionSource = tfInteractionSource,
                    textStyle = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        if (searchResult.scenes.isNotEmpty()) {
            item {
                SearchSection(
                    title = "Scenes",
                    onViewAllClick = { onViewAllClick("Scenes") },
                    startPadding = childPadding.start
                ) {
                    ScenesRow(
                        sceneList = searchResult.scenes,
                        onSceneSelected = onSceneClick,
                        startPadding = 0.dp
                    )
                }
            }
        }

        if (searchResult.performers.isNotEmpty()) {
            item {
                SearchSection(
                    title = "Performers",
                    onViewAllClick = { onViewAllClick("Performers") },
                    startPadding = childPadding.start
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(searchResult.performers) { performer ->
                            PerformerItem(performer)
                        }
                    }
                }
            }
        }

        if (searchResult.tags.isNotEmpty()) {
            item {
                SearchSection(
                    title = "Tags",
                    onViewAllClick = { onViewAllClick("Tags") },
                    startPadding = childPadding.start
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(searchResult.tags) { tag ->
                            TagItem(tag, onTagClick)
                        }
                    }
                }
            }
        }

        if (searchResult.groups.isNotEmpty()) {
            item {
                SearchSection(
                    title = "Groups",
                    onViewAllClick = { onViewAllClick("Groups") },
                    startPadding = childPadding.start
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(searchResult.groups) { group ->
                            GroupItem(group)
                        }
                    }
                }
            }
        }

        if (searchResult.markers.isNotEmpty()) {
            item {
                SearchSection(
                    title = "Markers",
                    onViewAllClick = { onViewAllClick("Markers") },
                    startPadding = childPadding.start
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(searchResult.markers) { marker ->
                            MarkerItem(marker)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSection(
    title: String,
    onViewAllClick: () -> Unit,
    startPadding: Dp,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = startPadding, bottom = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onViewAllClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "View All")
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
        content()
    }
}

@Composable
fun SceneItem(scene: Scene, onSceneClick: (Scene) -> Unit) {
    WideClassicCard(
        onClick = { onSceneClick(scene) },
        image = {
            AsyncImage(
                model = scene.posterUri,
                contentDescription = null,
                modifier = Modifier.aspectRatio(16/9f),
                contentScale = ContentScale.Fit
            )
        },
        title = { Text(text = scene.name) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PerformerItem(performer: SceneCast) {
    CompactCard(
        onClick = {},
        image = {
            AsyncImage(
                model = performer.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
        },
        title = { Text(text = performer.realName) },
        modifier = Modifier.width(120.dp)
    )
}

@Composable
fun TagItem(tag: SceneCategory, onClick: (String) -> Unit) {
    Card(
        onClick = { onClick(tag.id) },
        modifier = Modifier.width(150.dp).height(80.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = tag.name)
        }
    }
}

@Composable
fun GroupItem(group: SceneGroup) {
    WideClassicCard(
        onClick = {},
        image = {
            AsyncImage(
                model = group.posterUri,
                contentDescription = null,
                modifier = Modifier.aspectRatio(16/9f)
            )
        },
        title = { Text(text = group.name) },
        modifier = Modifier.width(200.dp)
    )
}

@Composable
fun MarkerItem(marker: SceneMarker) {
    WideClassicCard(
        onClick = {},
        image = {
            AsyncImage(
                model = marker.previewUri,
                contentDescription = null,
                modifier = Modifier.aspectRatio(16/9f)
            )
        },
        title = { Text(text = marker.title) },
        subtitle = { Text(text = marker.sceneTitle, modifier = Modifier.alpha(0.7f)) },
        modifier = Modifier.width(200.dp)
    )
}
