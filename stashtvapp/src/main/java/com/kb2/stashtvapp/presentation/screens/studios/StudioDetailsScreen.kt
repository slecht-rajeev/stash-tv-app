package com.kb2.stashtvapp.presentation.screens.studios

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kb2.stashtvapp.data.entities.Scene
import com.kb2.stashtvapp.data.entities.StudioDetails
import com.kb2.stashtvapp.data.util.StringConstants
import com.kb2.stashtvapp.presentation.common.Error
import com.kb2.stashtvapp.presentation.common.Loading
import com.kb2.stashtvapp.presentation.screens.dashboard.rememberChildPadding
import com.kb2.stashtvapp.presentation.theme.StashAppBorderWidth
import com.kb2.stashtvapp.presentation.theme.StashAppBottomListPadding
import com.kb2.stashtvapp.presentation.theme.StashAppCardShape
import com.kb2.stashtvapp.presentation.utils.focusOnInitialVisibility

object StudioDetailsScreen {
    const val StudioIdBundleKey = "studioId"
}

@Composable
fun StudioDetailsScreen(
    onBackPressed: () -> Unit,
    onSceneSelected: (Scene) -> Unit,
    studioDetailsScreenViewModel: StudioDetailsScreenViewModel = hiltViewModel()
) {
    val uiState by studioDetailsScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        StudioDetailsScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize())
        }

        StudioDetailsScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize())
        }

        is StudioDetailsScreenUiState.Done -> {
            val studioDetails = s.studioDetails
            StudioDetails(
                studioDetails = studioDetails,
                onBackPressed = onBackPressed,
                onSceneSelected = onSceneSelected
            )
        }
    }
}

@Composable
private fun StudioDetails(
    studioDetails: StudioDetails,
    onBackPressed: () -> Unit,
    onSceneSelected: (Scene) -> Unit,
    modifier: Modifier = Modifier
) {
    val childPadding = rememberChildPadding()
    val isFirstItemVisible = remember { mutableStateOf(false) }

    BackHandler(onBack = onBackPressed)

    Box(modifier = modifier.fillMaxSize()) {
        // Ambient Header Backdrop Image from First Scene if available
        val backdropUri = studioDetails.scenes.firstOrNull()?.posterUri
        if (!backdropUri.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(backdropUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark horizontal and vertical gradients
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color(0xFF0F1016).copy(alpha = 0.95f),
                                    Color(0xFF0F1016)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0F1016),
                                    Color.Transparent,
                                    Color(0xFF0F1016)
                                )
                            )
                        )
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Hero Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = childPadding.start,
                        end = childPadding.end,
                        top = childPadding.top,
                        bottom = 12.dp
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = onBackPressed,
                        colors = IconButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = studioDetails.name,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 34.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Scene Count Pill Badge
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                colors = SurfaceDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                ),
                                modifier = Modifier.padding(top = 2.dp)
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
                                        text = "${studioDetails.scenes.size} Scenes",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Browse scenes produced by ${studioDetails.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Scene Grid Section
            if (studioDetails.scenes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 64.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "No scenes found for this studio.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(
                        start = childPadding.start,
                        end = childPadding.end,
                        top = 12.dp,
                        bottom = StashAppBottomListPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        studioDetails.scenes,
                        key = { _, scene -> scene.id }
                    ) { index, scene ->
                        GridSceneCard(
                            scene = scene,
                            onClick = { onSceneSelected(scene) },
                            modifier = if (index == 0)
                                Modifier.focusOnInitialVisibility(isFirstItemVisible)
                            else Modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridSceneCard(
    scene: Scene,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
                        color = MaterialTheme.colorScheme.primary
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )

                val studio = scene.studioName?.ifBlank { null }
                if (studio != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        colors = SurfaceDefaults.colors(
                            containerColor = Color.Black.copy(alpha = 0.75f)
                        ),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
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
