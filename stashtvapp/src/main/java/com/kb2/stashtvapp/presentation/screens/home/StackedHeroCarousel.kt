@file:OptIn(ExperimentalTvMaterial3Api::class, UnstableApi::class)

package com.kb2.stashtvapp.presentation.screens.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.OptIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Text
import com.kb2.stashtvapp.R
import com.kb2.stashtvapp.data.entities.Scene
import com.kb2.stashtvapp.data.util.StringConstants
import com.kb2.stashtvapp.presentation.screens.videoPlayer.components.rememberPlayer
import com.kb2.stashtvapp.presentation.theme.StashAppButtonShape
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class, UnstableApi::class)
@Composable
fun StackedHeroCarousel(
    items: List<Scene>,
    modifier: Modifier = Modifier,
    onSceneClick: (Scene) -> Unit = {}
) {
    if (items.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    var isCarouselFocused by remember { mutableStateOf(false) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    LaunchedEffect(currentIndex, isCarouselFocused) {
        isPlayingPreview = false
        if (isCarouselFocused) {
            delay(3_000L)
            isPlayingPreview = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
            .onFocusChanged {
                isCarouselFocused = it.hasFocus
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                when (event.key) {
                    Key.DirectionRight -> {
                        if (currentIndex < items.lastIndex) {
                            currentIndex++
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionLeft -> {
                        if (currentIndex > 0) {
                            currentIndex--
                            true
                        } else {
                            false
                        }
                    }

                    Key.Enter,
                    Key.DirectionCenter -> {
                        onSceneClick(items[currentIndex])
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
        contentAlignment = Alignment.CenterStart
    ) {

        // Draw cards from back to front (maxIndex downTo minIndex) so active card remains on top.
        val minIndex = (currentIndex - 1).coerceAtLeast(0)
        val maxIndex = (currentIndex + 2).coerceAtMost(items.lastIndex)

        for (index in maxIndex downTo minIndex) {
            val scene = items[index]
            val depth = index - currentIndex

            key(scene.id) {
                StackedHeroCard(
                    scene = scene,
                    depth = depth,
                    isCarouselFocused = isCarouselFocused,
                    isPlayingPreview = (depth == 0 && isPlayingPreview),
                    onSceneClick = onSceneClick,
                    modifier = Modifier.zIndex(
                        if (depth < 0) 0f else (10 - depth).toFloat()
                    )
                )
            }
        }

        // Indicator row at bottom center
        StackedCarouselIndicator(
            itemCount = items.size,
            activeItemIndex = currentIndex,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .zIndex(20f)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StackedCarouselIndicator(
    itemCount: Int,
    activeItemIndex: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                shape = ShapeDefaults.ExtraSmall
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        CarouselDefaults.IndicatorRow(
            itemCount = itemCount,
            activeItemIndex = activeItemIndex
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StackedHeroCard(
    scene: Scene,
    depth: Int,
    isCarouselFocused: Boolean,
    isPlayingPreview: Boolean = false,
    modifier: Modifier = Modifier,
    onSceneClick: (Scene) -> Unit = {}
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    val borderWidth by animateDpAsState(
        targetValue = if (depth == 0) {
            if (isCarouselFocused) 2.dp else 1.dp
        } else {
            0.dp
        },
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "borderWidth"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (depth == 0) {
            if (isCarouselFocused) 1f else 0.4f
        } else {
            0f
        },
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "borderAlpha"
    )

    val animationSpec = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow
    )

    val translationX by animateFloatAsState(
        targetValue = when {
            depth < 0 -> -850f
            depth == 0 -> 0f
            depth == 1 -> 730f
            depth == 2 -> 850f
            else -> 1100f
        },
        animationSpec = animationSpec,
        label = "cardTranslation"
    )

    val scale by animateFloatAsState(
        targetValue = when {
            depth < 0 -> 0.90f
            depth == 0 -> 1f
            depth == 1 -> 0.90f
            depth == 2 -> 0.82f
            else -> 0.75f
        },
        animationSpec = animationSpec,
        label = "cardScale"
    )

    val alpha by animateFloatAsState(
        targetValue = when {
            depth < 0 -> 0f
            depth == 0 -> 1f
            depth == 1 -> 0.75f
            depth == 2 -> 0.45f
            else -> 0f
        },
        animationSpec = animationSpec,
        label = "cardAlpha"
    )

    val blurRadius by animateDpAsState(
        targetValue = when (depth) {
            0 -> 0.dp
            1 -> 80.dp
            2 -> 80.dp
            else -> 80.dp
        },
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardBlur"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isPlayingPreview) 0f else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "textAlpha"
    )

    val dimAlpha by animateFloatAsState(
        targetValue = when (depth) {
            0 -> 0f
            1 -> 0.55f
            2 -> 0.75f
            else -> 0.85f
        },
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dimAlpha"
    )

    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .width(800.dp)
            .height(350.dp)
            .graphicsLayer {
                this.translationX = translationX
                scaleX = scale
                scaleY = scale
                this.alpha = alpha

                // Scale toward the right-hand stack rather than center.
                transformOrigin =
                    androidx.compose.ui.graphics.TransformOrigin(
                        pivotFractionX = 0f,
                        pivotFractionY = 0.5f
                    )
            }
            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
            .clip(shape)
            .background(Color(0xFF171717))
            .then(
                if (borderWidth > 0.dp && borderAlpha > 0f) {
                    Modifier.border(
                        width = borderWidth,
                        color = Color.White.copy(alpha = borderAlpha),
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
    ) {

        if (isPlayingPreview) {
            VideoPreviewPlayer(
                videoUri = scene.previewUri?.ifBlank { null } ?: scene.videoUri,
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
                    }
            )
        } else {
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

        // Dimming scrim overlay for non-active cards
        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }

        // Foreground details column on the left side
        if (textAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = textAlpha },
                contentAlignment = Alignment.BottomStart
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .padding(24.dp),
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
                if (depth == 0) {
                    Button(
                        onClick = { onSceneClick(scene) },
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .height(34.dp),
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
                }
            }
        }
    }
}
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPreviewPlayer(
    videoUri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = rememberPlayer(context)

    LaunchedEffect(exoPlayer, videoUri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        exoPlayer.volume = 0f
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.prepare()
        exoPlayer.play()
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    PlayerSurface(
        player = exoPlayer,
        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
        modifier = modifier.resizeWithContentScale(
            contentScale = ContentScale.Crop,
            sourceSizeDp = null
        )
    )
}