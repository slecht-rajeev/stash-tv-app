package com.kb2.stashtvapp.presentation.screens.studios

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Hero Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = childPadding.start,
                    end = childPadding.end,
                    top = childPadding.top.times(1.5f),
                    bottom = 12.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = studioDetails.name,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Scene Count Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "${studioDetails.scenes.size} Scenes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Scene Grid
        if (studioDetails.scenes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
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
                    top = 8.dp,
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