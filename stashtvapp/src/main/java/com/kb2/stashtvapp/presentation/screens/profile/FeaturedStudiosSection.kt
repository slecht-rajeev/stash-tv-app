package com.kb2.stashtvapp.presentation.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.Checkbox
import androidx.tv.material3.Button
import androidx.tv.material3.CheckboxDefaults
import com.kb2.stashtvapp.data.local.PreferenceManager
import com.kb2.stashtvapp.data.repositories.StashGraphQLDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeaturedStudiosViewModel @Inject constructor(
    private val stashGraphQLDataSource: StashGraphQLDataSource,
    private val preferenceManager: PreferenceManager
) : ViewModel() {
    var studios by mutableStateOf<List<Pair<String, String>>>(emptyList())
    val selectedIds = mutableStateListOf<String>()
    var isSaving by mutableStateOf(false)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            studios = stashGraphQLDataSource.getStudios()
            selectedIds.clear()
            selectedIds.addAll(preferenceManager.getFeaturedStudioIds())
        }
    }

    fun toggleStudio(id: String) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
    }

    fun save() {
        isSaving = true
        preferenceManager.saveFeaturedStudioIds(selectedIds.toList())
        isSaving = false
    }
}

@Composable
fun FeaturedStudiosSection(
    viewModel: FeaturedStudiosViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp)
    ) {
        Text(
            text = "Select Featured Studios",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.studios) { (id, name) ->
                val isSelected = viewModel.selectedIds.contains(id)
                ListItem(
                    selected = false,
                    onClick = { viewModel.toggleStudio(id) },
                    headlineContent = {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    trailingContent = {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null, // Handled by ListItem onClick
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    },
                    colors = ListItemDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                        focusedContentColor = MaterialTheme.colorScheme.surface,
                    ),
                    shape = ListItemDefaults.shape(shape = MaterialTheme.shapes.small)
                )
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        Button(
            onClick = { viewModel.save() },
            modifier = Modifier.align(Alignment.End),
            enabled = !viewModel.isSaving
        ) {
            Text(text = if (viewModel.isSaving) "Saving..." else "Save Changes")
        }
    }
}
