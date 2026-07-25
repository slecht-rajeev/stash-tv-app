package com.kb2.stashtvapp.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kb2.stashtvapp.data.entities.Scene
import com.kb2.stashtvapp.presentation.theme.StashAppBorderWidth
import com.kb2.stashtvapp.presentation.theme.StashAppCardShape

@Composable
fun SceneCardSimple(
    scene: Scene,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .height(220.dp)
    ) {

        Column {

            // Row 1 - Image 16:9
            PosterImage(
                scene = scene,
                modifier = modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                    },
            )


            // Row 2 - Title
            Text(
                text = scene.name,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(
                        start = 12.dp,
                        top = 12.dp,
                        end = 12.dp
                    ),
                maxLines = 1
            )


            // Row 3 - Date
            Text(
                text = "2020-19-26",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(
                        start = 12.dp,
                        top = 6.dp,
                        end = 12.dp
                    )
            )


            // Row 4 - Description
            Text(
                text = scene.description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(
                        start = 12.dp,
                        top = 8.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    ),
                maxLines = 3
            )
        }
    }
}
