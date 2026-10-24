package com.kb2.stashtvapp.presentation.screens.scenes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kb2.stashtvapp.data.entities.SceneCategory
import com.kb2.stashtvapp.presentation.theme.StashAppBorderWidth
import com.kb2.stashtvapp.presentation.theme.StashAppButtonShape
import com.kb2.stashtvapp.presentation.theme.StashAppCardShape

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PositionsTagSelector(
    assignedTags: List<SceneCategory>,
    availablePositionSubTags: List<SceneCategory>,
    onAddPosition: (tagId: String) -> Unit,
    onRemovePosition: (tagId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Identify which assigned tags belong to the Positions parent
    val positionSubTagIds = availablePositionSubTags.map { it.id }.toSet()
    val assignedPositions = assignedTags.filter { positionSubTagIds.contains(it.id) }

    // 2. Identify available positions (not currently assigned)
    val assignedPositionIds = assignedPositions.map { it.id }.toSet()
    val availableOptions = availablePositionSubTags.filter { !assignedPositionIds.contains(it.id) }

    var isDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "POSITIONS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.secondary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Removable Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                assignedPositions.forEach { positionTag ->
                    PositionChip(
                        tag = positionTag,
                        onRemove = { onRemovePosition(positionTag.id) }
                    )
                }
            }

            // Dropdown Trigger Button
            if (availableOptions.isNotEmpty()) {
                Button(
                    onClick = { isDropdownOpen = true },
                    modifier = Modifier.height(34.dp),
                    shape = ButtonDefaults.shape(shape = StashAppButtonShape),
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface,
                        focusedContentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Position",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Add Position",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Dropdown Selection Dialog
    if (isDropdownOpen) {
        PositionDropdownDialog(
            options = availableOptions,
            onSelect = { selectedTagId ->
                isDropdownOpen = false
                onAddPosition(selectedTagId)
            },
            onDismiss = { isDropdownOpen = false }
        )
    }
}

@Composable
private fun PositionChip(
    tag: SceneCategory,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onRemove,
        modifier = modifier.height(32.dp),
        shape = ClickableSurfaceDefaults.shape(StashAppButtonShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.errorContainer
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = StashAppBorderWidth,
                    color = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = StashAppButtonShape
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove ${tag.name}",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PositionDropdownDialog(
    options: List<SceneCategory>,
    onSelect: (tagId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialFocusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .heightIn(max = 320.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Position",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options, key = { it.id }) { option ->
                        Surface(
                            onClick = { onSelect(option.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .focusRequester(initialFocusRequester),
                            shape = ClickableSurfaceDefaults.shape(StashAppButtonShape),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(
                                    border = BorderStroke(
                                        width = StashAppBorderWidth,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = StashAppButtonShape
                                )
                            ),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}