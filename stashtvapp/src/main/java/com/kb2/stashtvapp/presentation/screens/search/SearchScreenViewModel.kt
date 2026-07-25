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

package com.kb2.stashtvapp.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kb2.stashtvapp.data.entities.SceneList
import com.kb2.stashtvapp.data.entities.SearchResult
import com.kb2.stashtvapp.data.repositories.SceneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val sceneRepository: SceneRepository
) : ViewModel() {

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Done(SearchResult()))
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private var currentQuery = ""

    fun query(queryString: String) {
        currentQuery = queryString
        if (queryString.isBlank()) {
            _searchState.value = SearchState.Done(SearchResult())
            return
        }
        viewModelScope.launch { postQuery(queryString) }
    }

    private suspend fun postQuery(queryString: String) {
        _searchState.value = SearchState.Searching
        try {
            val result = sceneRepository.globalSearch(query = queryString, limit = 5)
            _searchState.value = SearchState.Done(result)
        } catch (e: Exception) {
            _searchState.value = SearchState.Error(e.message ?: "Unknown error")
        }
    }

    fun fetchAll(category: String) {
        if (currentQuery.isBlank()) return
        viewModelScope.launch {
            _searchState.value = SearchState.Searching
            try {
                // Fetch with large limit for the specific request
                // In a more complex app, we might only fetch the specific category.
                // For now, let's fetch all categories with a high limit if one is clicked.
                val result = sceneRepository.globalSearch(query = currentQuery, limit = 100)
                _searchState.value = SearchState.Done(result, expandedCategory = category)
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetExpansion() {
        if (currentQuery.isBlank()) return
        query(currentQuery)
    }
}

sealed interface SearchState {
    data object Searching : SearchState
    data class Done(val searchResult: SearchResult, val expandedCategory: String? = null) : SearchState
    data class Error(val message: String) : SearchState
}
