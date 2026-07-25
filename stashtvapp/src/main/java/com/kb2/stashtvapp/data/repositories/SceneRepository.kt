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

package com.kb2.stashtvapp.data.repositories

import com.kb2.stashtvapp.data.entities.SceneCategoryDetails
import com.kb2.stashtvapp.data.entities.SceneCategoryList
import com.kb2.stashtvapp.data.entities.SceneDetails
import com.kb2.stashtvapp.data.entities.SceneList
import com.kb2.stashtvapp.data.entities.HomeSection
import com.kb2.stashtvapp.data.entities.SearchResult
import com.kb2.stashtvapp.data.entities.Studio
import com.kb2.stashtvapp.data.entities.StudioDetails
import kotlinx.coroutines.flow.Flow

interface SceneRepository {
    fun getFeaturedScenes(): Flow<SceneList>
    fun getHomeSections(): Flow<List<HomeSection>>
    fun getTrendingScenes(): Flow<SceneList>
    fun getTop10Scenes(): Flow<SceneList>
    fun getNowPlayingScenes(): Flow<SceneList>
    fun getSceneCategories(): Flow<SceneCategoryList>
    fun getStudios(): Flow<List<Studio>>
    suspend fun getSceneCategoryDetails(categoryId: String): SceneCategoryDetails
    suspend fun getStudioDetails(studioId: String): StudioDetails
    suspend fun getSceneDetails(sceneId: String): SceneDetails
    suspend fun recordScenePlay(sceneId: String)
    suspend fun saveSceneActivity(sceneId: String, resumeTime: Double)
    suspend fun searchScenes(query: String): SceneList
    suspend fun globalSearch(query: String, limit: Int = 5): SearchResult
    fun getScenesWithLongThumbnail(): Flow<SceneList>
    fun getScenes(): Flow<SceneList>
    fun getScenesPaginated(page: Int, perPage: Int, randomized: Boolean = false): Flow<SceneList>
    fun getFavouriteScenes(): Flow<SceneList>
    fun getFavouriteScenesPaginated(page: Int, perPage: Int): Flow<SceneList>
    fun getWatchLaterScenesPaginated(page: Int, perPage: Int): Flow<SceneList>
    fun getPopularFilmsThisWeek(): Flow<SceneList>
    fun getBingeWatchDramas(): Flow<SceneList>
    fun getTVShows(): Flow<SceneList>
}
