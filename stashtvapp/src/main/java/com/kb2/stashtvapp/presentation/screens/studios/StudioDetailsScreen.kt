package com.kb2.stashtvapp.presentation.screens.studios

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kb2.stashtvapp.data.entities.Scene
import com.kb2.stashtvapp.data.entities.StudioDetails
import com.kb2.stashtvapp.presentation.common.Error
import com.kb2.stashtvapp.presentation.common.Loading
import com.kb2.stashtvapp.presentation.common.SceneCard
import com.kb2.stashtvapp.presentation.common.PosterImage
import com.kb2.stashtvapp.presentation.screens.dashboard.rememberChildPadding
import com.kb2.stashtvapp.presentation.theme.StashAppBottomListPadding
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = studioDetails.name,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(
                vertical = childPadding.top.times(3.5f)
            )
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(bottom = StashAppBottomListPadding)
        ) {
            itemsIndexed(
                studioDetails.scenes,
                key = { _, scene ->
                    scene.id
                }
            ) { index, scene ->
                SceneCard(
                    onClick = { onSceneSelected(scene) },
                    modifier = Modifier
                        .aspectRatio(16 / 9f)
                        .padding(8.dp)
                        .then(
                            if (index == 0)
                                Modifier.focusOnInitialVisibility(isFirstItemVisible)
                            else Modifier
                        ),
                ) {
                    PosterImage(scene = scene, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
