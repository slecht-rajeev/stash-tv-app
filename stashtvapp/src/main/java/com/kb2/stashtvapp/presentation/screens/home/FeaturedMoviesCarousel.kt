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

import android.view.KeyEvent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.Carousel
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.CarouselState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.kb2.stashtvapp.R
import com.kb2.stashtvapp.data.entities.Scene
import com.kb2.stashtvapp.data.util.StringConstants
import com.kb2.stashtvapp.presentation.theme.StashAppBorderWidth
import com.kb2.stashtvapp.presentation.theme.StashAppButtonShape
import com.kb2.stashtvapp.presentation.utils.Padding
import com.kb2.stashtvapp.presentation.utils.handleDPadKeyEvents

@OptIn(ExperimentalTvMaterial3Api::class)
val CarouselSaver = Saver<CarouselState, Int>(
    save = { it.activeItemIndex },
    restore = { CarouselState(it) }
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FeaturedScenesCarousel(
    scenes: List<Scene>,
    padding: Padding,
    goToVideoPlayer: (scene: Scene) -> Unit,
    onToggleFavorite: ((scene: Scene) -> Unit)? = null,
    onAddWatchLater: ((scene: Scene) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val carouselState = rememberSaveable(saver = CarouselSaver) { CarouselState(0) }
    var isCarouselFocused by remember { mutableStateOf(false) }
    val alpha = if (isCarouselFocused) {
        1f
    } else {
        0f
    }

    Carousel(
        modifier = modifier
            .padding(start = padding.start, end = padding.start, top = padding.top)
            .border(
                width = StashAppBorderWidth,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                shape = ShapeDefaults.Medium,
            )
            .clip(ShapeDefaults.Medium)
            .onFocusChanged {
                // Because the carousel itself never gets the focus
                isCarouselFocused = it.hasFocus
            }
            .semantics {
                contentDescription =
                    StringConstants.Composable.ContentDescription.ScenesCarousel
            },
        itemCount = scenes.size,
        carouselState = carouselState,
        carouselIndicator = {
            CarouselIndicator(
                itemCount = scenes.size,
                activeItemIndex = carouselState.activeItemIndex
            )
        },
        contentTransformStartToEnd = fadeIn(tween(durationMillis = 1000))
            .togetherWith(fadeOut(tween(durationMillis = 1000))),
        contentTransformEndToStart = fadeIn(tween(durationMillis = 1000))
            .togetherWith(fadeOut(tween(durationMillis = 1000))),
        content = { index ->
            val scene = scenes[index]
            // background
            CarouselItemBackground(scene = scene, modifier = Modifier.fillMaxSize())
            // foreground
            CarouselItemForeground(
                scene = scene,
                goToVideoPlayer = goToVideoPlayer,
                onToggleFavorite = onToggleFavorite,
                onAddWatchLater = onAddWatchLater,
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BoxScope.CarouselIndicator(
    itemCount: Int,
    activeItemIndex: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(32.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .graphicsLayer {
                clip = true
                shape = ShapeDefaults.ExtraSmall
            }
            .align(Alignment.BottomEnd)
    ) {
        CarouselDefaults.IndicatorRow(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            itemCount = itemCount,
            activeItemIndex = activeItemIndex,
        )
    }
}

@Composable
private fun CarouselItemForeground(
    scene: Scene,
    modifier: Modifier = Modifier,
    goToVideoPlayer: (scene: Scene) -> Unit = {},
    onToggleFavorite: ((scene: Scene) -> Unit)? = null,
    onAddWatchLater: ((scene: Scene) -> Unit)? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .padding(32.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            val studioText = scene.studioName?.ifBlank { null }
            if (studioText != null) {
                Text(
                    text = studioText.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.8f),
                            offset = Offset(x = 1f, y = 2f),
                            blurRadius = 3f
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                text = scene.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(x = 2f, y = 4f),
                        blurRadius = 2f
                    )
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (scene.description.isNotBlank()) {
                Text(
                    text = scene.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.75f
                        ),
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(x = 2f, y = 4f),
                            blurRadius = 2f
                        )
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            ActionButtonsRow(
                scene = scene,
                goToVideoPlayer = goToVideoPlayer,
                onToggleFavorite = onToggleFavorite,
                onAddWatchLater = onAddWatchLater
            )
        }
    }
}

@Composable
private fun CarouselItemBackground(scene: Scene, modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.background
    Box(modifier = modifier.background(backgroundColor)) {
        AsyncImage(
            model = scene.posterUri,
            contentDescription = StringConstants
                .Composable
                .ContentDescription
                .scenePoster(scene.name),
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.horizontalGradient(
                            colors = listOf(
                                backgroundColor,
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = size.width * 0.5f
                        )
                    )
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor.copy(alpha = 0.5f)
                            )
                        )
                    )
                },
            contentScale = ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionButtonsRow(
    scene: Scene,
    goToVideoPlayer: (scene: Scene) -> Unit,
    onToggleFavorite: ((scene: Scene) -> Unit)? = null,
    onAddWatchLater: ((scene: Scene) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFavorite by remember(scene.id) { mutableStateOf(false) }
    var isWatchLater by remember(scene.id) { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(top = 8.dp)
            .focusGroup()
    ) {
        Button(
            onClick = { goToVideoPlayer(scene) },
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            shape = ButtonDefaults.shape(shape = StashAppButtonShape),
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
                focusedContentColor = MaterialTheme.colorScheme.surface,
            ),
            scale = ButtonDefaults.scale(scale = 1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = stringResource(R.string.watch_now),
                style = MaterialTheme.typography.labelMedium
            )
        }

        IconButton(
            onClick = {
                isFavorite = !isFavorite
                onToggleFavorite?.invoke(scene)
            },
            modifier = Modifier.size(34.dp),
            shape = IconButtonDefaults.shape(shape = StashAppButtonShape),
            colors = IconButtonDefaults.colors(
                containerColor = if (isFavorite) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                contentColor = if (isFavorite) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.onSurface,
                focusedContentColor = MaterialTheme.colorScheme.surface,
            ),
            scale = IconButtonDefaults.scale(scale = 1f)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Add to Favorites",
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = {
                isWatchLater = !isWatchLater
                onAddWatchLater?.invoke(scene)
            },
            modifier = Modifier.size(34.dp),
            shape = IconButtonDefaults.shape(shape = StashAppButtonShape),
            colors = IconButtonDefaults.colors(
                containerColor = if (isWatchLater) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                contentColor = if (isWatchLater) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.onSurface,
                focusedContentColor = MaterialTheme.colorScheme.surface,
            ),
            scale = IconButtonDefaults.scale(scale = 1f)
        ) {
            Icon(
                imageVector = if (isWatchLater) Icons.Filled.Check else Icons.Outlined.Add,
                contentDescription = "Add to Watch Later",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}