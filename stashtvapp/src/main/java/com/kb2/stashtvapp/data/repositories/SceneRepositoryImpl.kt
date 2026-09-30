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

import android.util.Log
import com.apollographql.apollo3.api.Optional
import com.kb2.stashtvapp.data.entities.SceneCast
import com.kb2.stashtvapp.data.entities.SceneCategoryDetails
import com.kb2.stashtvapp.data.entities.SceneDetails
import com.kb2.stashtvapp.data.entities.SceneList
import com.kb2.stashtvapp.data.entities.HomeSection
import com.kb2.stashtvapp.data.entities.SceneReviewsAndRatings
import com.kb2.stashtvapp.type.FindFilterType
import com.kb2.stashtvapp.type.SortDirectionEnum
import com.kb2.stashtvapp.data.entities.SearchResult
import com.kb2.stashtvapp.data.entities.Studio
import com.kb2.stashtvapp.data.entities.StudioDetails
import com.kb2.stashtvapp.data.local.PreferenceManager
import com.kb2.stashtvapp.data.util.StringConstants
import com.kb2.stashtvapp.type.CriterionModifier
import com.kb2.stashtvapp.type.HierarchicalMultiCriterionInput
import com.kb2.stashtvapp.type.SceneFilterType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class SceneRepositoryImpl @Inject constructor(
    private val stashGraphQLDataSource: StashGraphQLDataSource,
    private val preferenceManager: PreferenceManager,
    private val tvDataSource: TvDataSource
) : SceneRepository {

    private val baseUrl = "http://192.168.1.19:9999"

    private fun String?.toFullUrl(): String {
        if (this == null) return ""
        if (startsWith("http")) return this
        return "$baseUrl$this"
    }

    override fun getFeaturedScenes() = flow {
        val featuredStudioIds = preferenceManager.getFeaturedStudioIds()
        val sceneFilter = if (featuredStudioIds.isNotEmpty()) {
            SceneFilterType(
                studios = Optional.present(
                    HierarchicalMultiCriterionInput(
                        value = Optional.present(featuredStudioIds),
                        modifier = CriterionModifier.INCLUDES
                    )
                )
            )
        } else {
            SceneFilterType(
                studios_filter = Optional.present(
                    com.kb2.stashtvapp.type.StudioFilterType(
                        favorite = Optional.present(true)
                    )
                )
            )
        }

        val findFilter = FindFilterType(
            per_page = Optional.present(100),
            sort = Optional.present("random")
        )

        var list = stashGraphQLDataSource.getScenes(filter = findFilter, sceneFilter = sceneFilter)
        if (list.isEmpty() && featuredStudioIds.isEmpty()) {
            list = stashGraphQLDataSource.getScenes(filter = findFilter)
        }

        emit(list.shuffled().take(8))
    }

    override fun getHomeSections(): Flow<List<HomeSection>> = flow {
        val uiConfig = stashGraphQLDataSource.getUIConfig()
        var frontPageContent = uiConfig?.get("frontPageContent") as? List<*> ?: uiConfig?.get("front_page_content") as? List<*>
        
        if (frontPageContent == null) {
            val interfaceConfig = uiConfig?.get("interface") as? Map<String, Any?>
            frontPageContent = interfaceConfig?.get("frontPageContent") as? List<*> ?: interfaceConfig?.get("front_page_content") as? List<*>
        }
        
        val sections = mutableListOf<HomeSection>()
        
        frontPageContent?.forEach { item ->
            val map = item as? Map<String, Any?>
            val type = (map?.get("__typename") ?: map?.get("sectionType"))?.toString()
            
            if (type == "saved_filter" || type == "SavedFilter") {
                val id = map?.get("saved_filter_id")?.toString() ?: map?.get("savedFilterId")?.toString()
                if (id != null) {
                    val sf = stashGraphQLDataSource.getSavedFilter(id)
                    if (sf != null && sf.mode.name == "SCENES") {
                        val scenes = stashGraphQLDataSource.getScenes(
                            filter = FindFilterType(
                                q = Optional.presentIfNotNull(sf.find_filter?.q),
                                sort = Optional.presentIfNotNull(sf.find_filter?.sort),
                                direction = Optional.presentIfNotNull(sf.find_filter?.direction?.let { 
                                    try { SortDirectionEnum.valueOf(it.name) } catch(e: Exception) { null }
                                }),
                                per_page = Optional.present(20)
                            )
                        )
                        if (scenes.isNotEmpty()) {
                            sections.add(HomeSection(title = sf.name, scenes = scenes))
                        }
                    }
                }
                else {
                    println("StashHome: Warning - saved_filter section missing id: $map")

                }
            } else if (type == "scenes" || type == "RecentScenes") {
                val title = map?.get("title")?.toString() ?: map?.get("name")?.toString() ?: "Recent Scenes"
                val scenes = stashGraphQLDataSource.getScenes(
                    filter = FindFilterType(per_page = Optional.present(20))
                )
                if (scenes.isNotEmpty()) {
                    sections.add(HomeSection(title = title, scenes = scenes))
                }
            }
        }
        
        if (sections.isEmpty()) {
            println("StashHome: No sections found in config, using fallback Trending row.")
            sections.add(HomeSection("Trending", stashGraphQLDataSource.getScenes().take(20)))
        }
        
        emit(sections)
    }

    override fun getTrendingScenes(): Flow<SceneList> = flow {
        emit(stashGraphQLDataSource.getScenes())
    }

    override fun getTop10Scenes(): Flow<SceneList> = flow {
        emit(stashGraphQLDataSource.getScenes().take(10))
    }

    override fun getNowPlayingScenes(): Flow<SceneList> = flow {
        emit(stashGraphQLDataSource.getScenes())
    }

    override fun getSceneCategories() = flow {
        emit(stashGraphQLDataSource.getTags())
    }

    override fun getStudios() = flow {
        emit(stashGraphQLDataSource.getStudios().map { Studio(it.first, it.second) })
    }

    override suspend fun getSceneCategoryDetails(categoryId: String): SceneCategoryDetails {
        val tags = stashGraphQLDataSource.getTags()
        val tag = tags.find { it.id == categoryId } ?: tags.first()
        val filter = SceneFilterType(
            tags = Optional.present(
                HierarchicalMultiCriterionInput(
                    value = Optional.present(listOf(categoryId)),
                    modifier = CriterionModifier.INCLUDES
                )
            )
        )
        val scenes = stashGraphQLDataSource.getScenes(sceneFilter = filter)

        return SceneCategoryDetails(
            id = tag.id,
            name = tag.name,
            scenes = scenes
        )
    }

    override suspend fun getStudioDetails(studioId: String): StudioDetails {
        val studios = stashGraphQLDataSource.getStudios()
        val studioPair = studios.find { it.first == studioId } ?: studios.first()
        val filter = SceneFilterType(
            studios = Optional.present(
                HierarchicalMultiCriterionInput(
                    value = Optional.present(listOf(studioId)),
                    modifier = CriterionModifier.INCLUDES
                )
            )
        )
        val scenes = stashGraphQLDataSource.getScenes(sceneFilter = filter)

        return StudioDetails(
            id = studioPair.first,
            name = studioPair.second,
            scenes = scenes
        )
    }

    override suspend fun getSceneDetails(sceneId: String): SceneDetails {
        val sceneData = stashGraphQLDataSource.getSceneDetails(sceneId)
        val allScenes = stashGraphQLDataSource.getScenes()

        println("Scene Id: $sceneId")
        val scene = sceneData ?: throw Exception("Scene not found")

        val title = if (!scene.title.isNullOrBlank()) {
            scene.title
        } else {
            scene.files.firstOrNull()?.basename?.ifBlank { null } ?: "No Title"
        }

        return SceneDetails(
            id = scene.id,
            videoUri = scene.paths.stream.toFullUrl(),
            subtitleUri = null,
            posterUri = scene.paths.screenshot.toFullUrl(),
            name = title,
            description = scene.details ?: "",
            pgRating = "NC-17",
            releaseDate = scene.date ?: "Unknown",
            categories = scene.tags.map { it.name },
            duration = "N/A",
            director = scene.studio?.name ?: "Unknown",
            screenplay = "N/A",
            music = "N/A",
            castAndCrew = scene.performers.map {
                SceneCast(
                    id = it.id,
                    characterName = it.name,
                    realName = it.name,
                    avatarUrl = it.image_path.toFullUrl()
                )
            },
            status = "Available",
            originalLanguage = "English",
            budget = "N/A",
            revenue = "N/A",
            similarScenes = allScenes.shuffled().take(10),
            reviewsAndRatings = emptyList(),
            resumeTime = scene.resume_time ?: 0.0
        )
    }

    override suspend fun recordScenePlay(sceneId: String) {
        stashGraphQLDataSource.recordScenePlay(sceneId)
    }

    override suspend fun saveSceneActivity(sceneId: String, resumeTime: Double) {
        stashGraphQLDataSource.saveSceneActivity(sceneId, resumeTime)
    }

    override suspend fun addFavorite(sceneId: String) {
        stashGraphQLDataSource.addFavorite(sceneId)
    }

    override suspend fun addWatchLaterTag(sceneId: String) {
        stashGraphQLDataSource.addWatchLaterTag(sceneId)
    }

    override suspend fun searchScenes(query: String): SceneList {
        return stashGraphQLDataSource.getScenes().filter {
            it.name.contains(other = query, ignoreCase = true)
        }
    }

    override suspend fun globalSearch(query: String, limit: Int): SearchResult {
        return stashGraphQLDataSource.globalSearch(query, limit)
    }

    override fun getScenesWithLongThumbnail() = flow {
        emit(stashGraphQLDataSource.getScenes())
    }

    override fun getScenes(): Flow<SceneList> = flow {
        emit(stashGraphQLDataSource.getScenes())
    }

    override fun getScenesPaginated(page: Int, perPage: Int, randomized: Boolean): Flow<SceneList> = flow {
        val filter = FindFilterType(
            page = Optional.present(page),
            per_page = Optional.present(perPage),
            sort = if (randomized) Optional.present("random") else Optional.Absent
        )
        emit(stashGraphQLDataSource.getScenes(filter = filter))
    }

    override fun getFavouriteScenes(): Flow<SceneList> = flow {
        emit(stashGraphQLDataSource.getScenes())
    }

    override fun getFavouriteScenesPaginated(page: Int, perPage: Int): Flow<SceneList> = flow {
        val filter = FindFilterType(
            page = Optional.present(page),
            per_page = Optional.present(perPage)
        )
        // TODO: Apply actual favourite filter if criteria is known (e.g. rating > 4)
        emit(stashGraphQLDataSource.getScenes(filter = filter))
    }

    override fun getWatchLaterScenesPaginated(page: Int, perPage: Int): Flow<SceneList> = flow {
        val filter = FindFilterType(
            page = Optional.present(page),
            per_page = Optional.present(perPage)
        )
        // TODO: Apply actual watch later filter (e.g. specific tag)
        emit(stashGraphQLDataSource.getScenes(filter = filter))
    }

    override fun getPopularFilmsThisWeek(): Flow<SceneList> = flow {
        emit(stashGraphQLDataSource.getScenes())
    }

    override fun getBingeWatchDramas(): Flow<SceneList> = flow {
        emit(tvDataSource.getBingeWatchDramaList())
    }

    override fun getTVShows(): Flow<SceneList> = flow {
        emit(tvDataSource.getTvShowList())
    }
}
