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

import android.view.KeyEvent
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kb2.stashtvapp.R
import com.kb2.stashtvapp.data.entities.SceneDetails
import com.kb2.stashtvapp.data.util.StringConstants
import com.kb2.stashtvapp.presentation.screens.dashboard.rememberChildPadding
import com.kb2.stashtvapp.presentation.theme.StashAppButtonShape

@Composable
fun SceneDetails(
    sceneDetails: SceneDetails,
    goToScenePlayer: (startPositionMs: Long) -> Unit,
    availablePositionSubTags: List<com.kb2.stashtvapp.data.entities.SceneCategory> = emptyList(),
    onAddPositionTag: (tagId: String) -> Unit = {},
    onRemovePositionTag: (tagId: String) -> Unit = {},
    onToggleFavorite: (sceneId: String) -> Unit = {},
    onAddWatchLater: (sceneId: String) -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    val childPadding = rememberChildPadding()

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        SceneImageWithGradients(
            sceneDetails = sceneDetails,
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
        )

        Column(modifier = Modifier.fillMaxWidth(0.68f)) {
            // Visible Top Bar Menu with Back Button (Ensures top menu is never hidden!)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = childPadding.start,
                        end = childPadding.end,
                        top = childPadding.top,
                        bottom = 12.dp
                    )
            ) {
                IconButton(
                    onClick = onBackPressed,
                    colors = IconButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Video Details",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(start = childPadding.start)
            ) {
                val studio = sceneDetails.director.ifBlank { null }
                if (studio != null && studio != "Unknown") {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        colors = SurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = studio.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                SceneLargeTitle(sceneTitle = sceneDetails.name)

                Column(
                    modifier = Modifier.alpha(0.85f)
                ) {
                    SceneDescription(description = sceneDetails.description)
                    DotSeparatedRow(
                        modifier = Modifier.padding(top = 14.dp),
                        texts = listOf(
                            sceneDetails.pgRating,
                            sceneDetails.releaseDate,
                            sceneDetails.categories.joinToString(", "),
                            sceneDetails.duration
                        )
                    )
                    DirectorScreenplayMusicRow(
                        director = sceneDetails.director,
                        screenplay = sceneDetails.screenplay,
                        music = sceneDetails.music
                    )
                }

                ActionButtonsRow(
                    sceneDetails = sceneDetails,
                    goToScenePlayer = goToScenePlayer,
                    onToggleFavorite = onToggleFavorite,
                    onAddWatchLater = onAddWatchLater
                )

                if (availablePositionSubTags.isNotEmpty()) {
                    PositionsTagSelector(
                        assignedTags = sceneDetails.tags,
                        availablePositionSubTags = availablePositionSubTags,
                        onAddPosition = onAddPositionTag,
                        onRemovePosition = onRemovePositionTag,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

private fun formatResumeTime(seconds: Double): String {
    val totalSeconds = seconds.toLong()
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return if (mins >= 60) {
        val hours = mins / 60
        val remMins = mins % 60
        String.format("%d:%02d:%02d", hours, remMins, secs)
    } else {
        String.format("%d:%02d", mins, secs)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionButtonsRow(
    sceneDetails: SceneDetails,
    goToScenePlayer: (startPositionMs: Long) -> Unit,
    onToggleFavorite: (sceneId: String) -> Unit = {},
    onAddWatchLater: (sceneId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isFavorite by remember(sceneDetails.id) { mutableStateOf(false) }
    var isWatchLater by remember(sceneDetails.id) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val resumeTimeSec = sceneDetails.resumeTime
    val hasResumeProgress = resumeTimeSec > 5.0
    val resumePositionMs = (resumeTimeSec * 1000).toLong()

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(top = 20.dp)
            .focusGroup()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                            val moved = focusManager.moveFocus(FocusDirection.Right)
                            if (moved) return@onPreviewKeyEvent true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                            val moved = focusManager.moveFocus(FocusDirection.Left)
                            if (moved) return@onPreviewKeyEvent true
                        }
                    }
                }
                false
            }
    ) {
        if (hasResumeProgress) {
            val formattedTime = formatResumeTime(resumeTimeSec)

            // 1. Resume Button
            Button(
                onClick = { goToScenePlayer(resumePositionMs) },
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                shape = ButtonDefaults.shape(shape = StashAppButtonShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Resume from $formattedTime",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Start Over Button
            Button(
                onClick = { goToScenePlayer(0L) },
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                shape = ButtonDefaults.shape(shape = StashAppButtonShape),
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface,
                    focusedContentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Start Over",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Start Over",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        } else {
            // Standard Watch Now Button
            Button(
                onClick = { goToScenePlayer(0L) },
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                shape = ButtonDefaults.shape(shape = StashAppButtonShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.watch_now),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        IconButton(
            onClick = {
                isFavorite = !isFavorite
                onToggleFavorite(sceneDetails.id)
            },
            modifier = Modifier.size(40.dp),
            shape = IconButtonDefaults.shape(shape = StashAppButtonShape),
            colors = IconButtonDefaults.colors(
                containerColor = if (isFavorite) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = if (isFavorite) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.onSurface,
                focusedContentColor = MaterialTheme.colorScheme.surface,
            )
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Add to Favorites",
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = {
                isWatchLater = !isWatchLater
                onAddWatchLater(sceneDetails.id)
            },
            modifier = Modifier.size(40.dp),
            shape = IconButtonDefaults.shape(shape = StashAppButtonShape),
            colors = IconButtonDefaults.colors(
                containerColor = if (isWatchLater) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = if (isWatchLater) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.onSurface,
                focusedContentColor = MaterialTheme.colorScheme.surface,
            )
        ) {
            Icon(
                imageVector = if (isWatchLater) Icons.Filled.Check else Icons.Outlined.Add,
                contentDescription = "Add to Watch Later",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DirectorScreenplayMusicRow(
    director: String,
    screenplay: String,
    music: String
) {
    Row(modifier = Modifier.padding(top = 24.dp)) {
        TitleValueText(
            modifier = Modifier
                .padding(end = 24.dp)
                .weight(1f),
            title = stringResource(R.string.director),
            value = director
        )

        TitleValueText(
            modifier = Modifier
                .padding(end = 24.dp)
                .weight(1f),
            title = stringResource(R.string.screenplay),
            value = screenplay
        )

        TitleValueText(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.music),
            value = music
        )
    }
}

@Composable
private fun SceneDescription(description: String) {
    Text(
        text = description,
        style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp
        ),
        modifier = Modifier.padding(top = 8.dp),
        maxLines = 3
    )
}

@Composable
private fun SceneLargeTitle(sceneTitle: String) {
    Text(
        text = sceneTitle,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp
        ),
        maxLines = 1
    )
}

@Composable
private fun SceneImageWithGradients(
    sceneDetails: SceneDetails,
    modifier: Modifier = Modifier,
    gradientColor: Color = MaterialTheme.colorScheme.surface,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(sceneDetails.posterUri)
            .crossfade(true)
            .build(),
        contentDescription = StringConstants
            .Composable
            .ContentDescription
            .scenePoster(sceneDetails.name),
        contentScale = ContentScale.Crop,
        modifier = modifier.drawWithContent {
            drawContent()
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, gradientColor),
                    startY = size.height * 0.3f
                )
            )
            drawRect(
                Brush.horizontalGradient(
                    colors = listOf(gradientColor, Color.Transparent),
                    endX = size.width * 0.85f,
                    startX = size.width * 0.12f
                )
            )
            drawRect(
                Brush.linearGradient(
                    colors = listOf(gradientColor, Color.Transparent),
                    start = Offset(x = size.width * 0.35f, y = size.height * 0.35f),
                    end = Offset(x = size.width * 0.85f, y = 0f)
                )
            )
        }
    )
}
