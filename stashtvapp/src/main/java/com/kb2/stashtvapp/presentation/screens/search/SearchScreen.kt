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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kb2.stashtvapp.R
import com.kb2.stashtvapp.data.entities.Scene
import com.kb2.stashtvapp.data.entities.SceneCast
import com.kb2.stashtvapp.data.entities.SceneCategory
import com.kb2.stashtvapp.data.entities.SceneGroup
import com.kb2.stashtvapp.data.entities.SceneMarker
import com.kb2.stashtvapp.data.entities.SearchResult
import com.kb2.stashtvapp.data.util.StringConstants
import com.kb2.stashtvapp.presentation.common.Loading
import com.kb2.stashtvapp.presentation.screens.dashboard.rememberChildPadding
import com.kb2.stashtvapp.presentation.theme.StashAppBorderWidth
import com.kb2.stashtvapp.presentation.theme.StashAppBottomListPadding
import com.kb2.stashtvapp.presentation.theme.StashAppButtonShape
import com.kb2.stashtvapp.presentation.theme.StashAppCardShape
import com.kb2.stashtvapp.presentation.utils.focusOnInitialVisibility

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
            Loading(modifier = Modifier.fillMaxSize())
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
                modifier = Modifier.fillMaxSize(),
                lazyColumnState = lazyColumnState
            )
        }

        is SearchState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Error: ${s.message}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
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
    lazyColumnState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
) {
    val childPadding = rememberChildPadding()
    var searchQuery by remember { mutableStateOf("") }
    val tfFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val tfInteractionSource = remember { MutableInteractionSource() }
    val isTfFocused by tfInteractionSource.collectIsFocusedAsState()

    // Handle Expanded Category Grid View
    if (expandedCategory != null) {
        ExpandedCategoryView(
            category = expandedCategory,
            searchResult = searchResult,
            onBackFromExpansion = onBackFromExpansion,
            onSceneClick = onSceneClick,
            onTagClick = onTagClick
        )
        return
    }

    val isResultEmpty = searchQuery.isNotBlank() &&
        searchResult.scenes.isEmpty() &&
        searchResult.performers.isEmpty() &&
        searchResult.tags.isEmpty() &&
        searchResult.groups.isEmpty() &&
        searchResult.markers.isEmpty()

    LazyColumn(
        modifier = modifier,
        state = lazyColumnState,
        contentPadding = PaddingValues(bottom = StashAppBottomListPadding)
    ) {
        // Search Bar Item
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = childPadding.start)
                    .padding(top = 16.dp, bottom = 8.dp)
                    .background(
                        color = if (isTfFocused) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = StashAppCardShape
                    )
                    .border(
                        border = BorderStroke(
                            width = if (isTfFocused) 2.dp else 1.dp,
                            color = animateColorAsState(
                                targetValue = if (isTfFocused) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.border,
                                label = ""
                            ).value
                        ),
                        shape = StashAppCardShape
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { updatedQuery ->
                            searchQuery = updatedQuery
                            searchScenes(updatedQuery)
                        },
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.search_screen_et_placeholder),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(tfFocusRequester)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            focusManager.moveFocus(FocusDirection.Down)
                                            return@onKeyEvent true
                                        }
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            focusManager.moveFocus(FocusDirection.Up)
                                            return@onKeyEvent true
                                        }
                                    }
                                }
                                false
                            },
                        cursorBrush = Brush.verticalGradient(
                            colors = listOf(
                                LocalContentColor.current,
                                LocalContentColor.current
                            )
                        ),
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { searchScenes(searchQuery) }
                        ),
                        maxLines = 1,
                        interactionSource = tfInteractionSource,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                searchScenes("")
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Empty Search Prompt State
        if (searchQuery.isBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer { alpha = 0.35f }
                        )
                        Text(
                            text = "Search scenes, performers, tags, groups, or markers",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else if (isResultEmpty) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found for \"$searchQuery\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // 1. Scenes Section
        if (searchResult.scenes.isNotEmpty()) {
            item {
                SearchSection(
                    title = "Scenes",
                    count = searchResult.scenes.size,
                    onViewAllClick = { onViewAllClick("Scenes") },
                    startPadding = childPadding.start
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(start = childPadding.start, end = childPadding.end),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(searchResult.scenes, key = { it.id }) { scene ->
                            SearchSceneTile(scene = scene, onClick = { onSceneClick(scene) })
                        }
                    }
                }
            }
        }

        // 2. Performers Section
        if (searchResult.performers.isNotEmpty()) {
            item {
                SearchSection(
                    title = "Performers",
                    count = searchResult.performers.size,
                    onViewAllClick = { onViewAllClick("Performers") },
                    startPadding = childPadding.start
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(start = childPadding.start, end = childPadding.end),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(searchResult.performers, key = { it.id }) { performer ->
                            SearchPerformerTile(performer = performer)
                        }
                    }
                }
            }
        }

        // 3. Tags Section
        if (searchResult.tags.isNotEmpty()) {
            item {
                SearchSection(
                    title = "Tags",
                    count = searchResult.tags.size,
                    onViewAllClick = { onViewAllClick("Tags") },
                    startPadding = childPadding.start
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(start = childPadding.start, end = childPadding.end),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(searchResult.tags, key = { it.id }) { tag ->
                            SearchTagTile(tag = tag, onClick = { onTagClick(tag.id) })
                        }
                    }
                }
            }
        }

        // 4. Groups Section
        if (searchResult.groups.isNotEmpty()) {
            item {
                SearchSection(
                    title = "Groups",
                    count = searchResult.groups.size,
                    onViewAllClick = { onViewAllClick("Groups") },
                    startPadding = childPadding.start
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(start = childPadding.start, end = childPadding.end),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(searchResult.groups, key = { it.id }) { group ->
                            SearchGroupTile(group = group)
                        }
                    }
                }
            }
        }

        // 5. Markers Section
        if (searchResult.markers.isNotEmpty()) {
            item {
                SearchSection(
                    title = "Markers",
                    count = searchResult.markers.size,
                    onViewAllClick = { onViewAllClick("Markers") },
                    startPadding = childPadding.start
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(start = childPadding.start, end = childPadding.end),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(searchResult.markers, key = { it.id }) { marker ->
                            SearchMarkerTile(marker = marker)
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
    count: Int,
    onViewAllClick: () -> Unit,
    startPadding: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(top = 28.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding, end = startPadding, bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Button(
                onClick = onViewAllClick,
                modifier = Modifier.height(32.dp),
                shape = ButtonDefaults.shape(shape = StashAppButtonShape),
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface,
                    focusedContentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        content()
    }
}

@Composable
private fun ExpandedCategoryView(
    category: String,
    searchResult: SearchResult,
    onBackFromExpansion: () -> Unit,
    onSceneClick: (Scene) -> Unit,
    onTagClick: (String) -> Unit
) {
    val childPadding = rememberChildPadding()
    val isFirstItemVisible = remember { mutableStateOf(false) }

    BackHandler(onBack = onBackFromExpansion)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = childPadding.start,
                    end = childPadding.end,
                    top = childPadding.top.times(1.5f),
                    bottom = 16.dp
                )
        ) {
            Button(
                onClick = onBackFromExpansion,
                modifier = Modifier.height(36.dp),
                shape = ButtonDefaults.shape(shape = StashAppButtonShape)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Text(
                text = "All $category",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                )
            )
        }

        // Expanded Grid
        when (category) {
            "Scenes" -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(
                        start = childPadding.start,
                        end = childPadding.end,
                        bottom = StashAppBottomListPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(searchResult.scenes, key = { _, s -> s.id }) { index, scene ->
                        SearchSceneTile(
                            scene = scene,
                            onClick = { onSceneClick(scene) },
                            modifier = if (index == 0) Modifier.focusOnInitialVisibility(isFirstItemVisible) else Modifier
                        )
                    }
                }
            }

            "Performers" -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(
                        start = childPadding.start,
                        end = childPadding.end,
                        bottom = StashAppBottomListPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(searchResult.performers, key = { _, p -> p.id }) { index, performer ->
                        SearchPerformerTile(
                            performer = performer,
                            modifier = if (index == 0) Modifier.focusOnInitialVisibility(isFirstItemVisible) else Modifier
                        )
                    }
                }
            }

            "Tags" -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(
                        start = childPadding.start,
                        end = childPadding.end,
                        bottom = StashAppBottomListPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(searchResult.tags, key = { _, t -> t.id }) { index, tag ->
                        SearchTagTile(
                            tag = tag,
                            onClick = { onTagClick(tag.id) },
                            modifier = if (index == 0) Modifier.focusOnInitialVisibility(isFirstItemVisible) else Modifier
                        )
                    }
                }
            }

            "Groups" -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(
                        start = childPadding.start,
                        end = childPadding.end,
                        bottom = StashAppBottomListPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(searchResult.groups, key = { _, g -> g.id }) { index, group ->
                        SearchGroupTile(
                            group = group,
                            modifier = if (index == 0) Modifier.focusOnInitialVisibility(isFirstItemVisible) else Modifier
                        )
                    }
                }
            }

            "Markers" -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(
                        start = childPadding.start,
                        end = childPadding.end,
                        bottom = StashAppBottomListPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(searchResult.markers, key = { _, m -> m.id }) { index, marker ->
                        SearchMarkerTile(
                            marker = marker,
                            modifier = if (index == 0) Modifier.focusOnInitialVisibility(isFirstItemVisible) else Modifier
                        )
                    }
                }
            }
        }
    }
}

// ---------------- Tiles & Cards ----------------

@Composable
fun SearchSceneTile(scene: Scene, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(220.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = ClickableSurfaceDefaults.shape(StashAppCardShape),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(
                        width = StashAppBorderWidth,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = StashAppCardShape
                )
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(scene.posterUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = StringConstants.Composable.ContentDescription.scenePoster(scene.name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                val studio = scene.studioName?.ifBlank { null }
                if (studio != null) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        colors = SurfaceDefaults.colors(
                            containerColor = Color.Black.copy(alpha = 0.75f)
                        ),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = studio.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = scene.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!scene.studioName.isNullOrBlank()) {
                Text(
                    text = scene.studioName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SearchPerformerTile(performer: SceneCast, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.width(110.dp)
    ) {
        Surface(
            onClick = {},
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(
                        width = StashAppBorderWidth,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = CircleShape
                )
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(performer.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = performer.realName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = performer.realName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SearchTagTile(tag: SceneCategory, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = { onClick(tag.id) },
        modifier = modifier
            .width(180.dp)
            .height(95.dp),
        shape = ClickableSurfaceDefaults.shape(StashAppCardShape),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = StashAppBorderWidth,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = StashAppCardShape
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!tag.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(tag.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = tag.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    ),
                                    startY = size.height * 0.3f
                                )
                            )
                        }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                )
            }

            Text(
                text = tag.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = Offset(1f, 2f),
                        blurRadius = 3f
                    )
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun SearchGroupTile(group: SceneGroup, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(220.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = ClickableSurfaceDefaults.shape(StashAppCardShape),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(
                        width = StashAppBorderWidth,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = StashAppCardShape
                )
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(group.posterUri)
                    .crossfade(true)
                    .build(),
                contentDescription = group.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = group.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SearchMarkerTile(marker: SceneMarker, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(220.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = ClickableSurfaceDefaults.shape(StashAppCardShape),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(
                        width = StashAppBorderWidth,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = StashAppCardShape
                )
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(marker.previewUri)
                    .crossfade(true)
                    .build(),
                contentDescription = marker.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = marker.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (marker.sceneTitle.isNotBlank()) {
                Text(
                    text = marker.sceneTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}