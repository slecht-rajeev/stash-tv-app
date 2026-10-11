package com.kb2.stashtvapp.presentation.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kb2.stashtvapp.data.entities.SceneCategory
import com.kb2.stashtvapp.presentation.screens.dashboard.rememberChildPadding
import com.kb2.stashtvapp.presentation.theme.StashAppBorderWidth
import com.kb2.stashtvapp.presentation.theme.StashAppCardShape

@Composable
fun PositionsSection(
    subTags: List<SceneCategory>,
    onSubTagClick: (subTagId: String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Positions"
) {
    if (subTags.isEmpty()) return

    val childPadding = rememberChildPadding()

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 30.sp
            ),
            modifier = Modifier
                .padding(start = childPadding.start)
                .padding(vertical = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(start = childPadding.start, end = childPadding.end),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(subTags, key = { it.id }) { subTag ->
                PositionTile(
                    subTag = subTag,
                    onClick = { onSubTagClick(subTag.id) }
                )
            }
        }
    }
}

@Composable
private fun PositionTile(
    subTag: SceneCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(200.dp)
            .height(120.dp),
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
            if (!subTag.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(subTag.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = subTag.name,
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
                text = subTag.name,
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