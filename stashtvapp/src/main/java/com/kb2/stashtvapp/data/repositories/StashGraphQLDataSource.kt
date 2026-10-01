package com.kb2.stashtvapp.data.repositories

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.kb2.stashtvapp.FindPerformersQuery
import com.kb2.stashtvapp.FindSceneQuery
import com.kb2.stashtvapp.FindScenesQuery
import com.kb2.stashtvapp.FindStudiosQuery
import com.kb2.stashtvapp.FindTagsQuery
import com.kb2.stashtvapp.GetSavedFilterQuery
import com.kb2.stashtvapp.GetUIConfigQuery
import com.kb2.stashtvapp.SaveSceneActivityMutation
import com.kb2.stashtvapp.SearchGroupsQuery
import com.kb2.stashtvapp.SearchMarkersQuery
import com.kb2.stashtvapp.SearchPerformersQuery
import com.kb2.stashtvapp.SearchScenesQuery
import com.kb2.stashtvapp.SearchTagsQuery
import com.kb2.stashtvapp.data.entities.Scene
import com.kb2.stashtvapp.data.entities.SceneCast
import com.kb2.stashtvapp.data.entities.SceneCategory
import com.kb2.stashtvapp.data.entities.SceneGroup
import com.kb2.stashtvapp.data.entities.SceneMarker
import com.kb2.stashtvapp.data.entities.SearchResult
import com.kb2.stashtvapp.type.FindFilterType
import com.kb2.stashtvapp.type.GroupFilterType
import com.kb2.stashtvapp.type.PerformerFilterType
import com.kb2.stashtvapp.type.SceneFilterType
import com.kb2.stashtvapp.type.StringCriterionInput
import com.kb2.stashtvapp.type.TagFilterType
import javax.inject.Inject
import javax.inject.Singleton
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class StashGraphQLDataSource @Inject constructor(
    private val apolloClient: ApolloClient
) {
    private val baseUrl = "http://192.168.1.19:9999"

    private fun String?.toFullUrl(): String {
        if (this == null) return ""
        if (startsWith("http")) return this
        return "$baseUrl$this"
    }

    suspend fun getScenes(
        filter: FindFilterType? = null,
        sceneFilter: SceneFilterType? = null
    ): List<Scene> {
        val response = apolloClient.query(
            FindScenesQuery(
                filter = Optional.presentIfNotNull(filter),
                scene_filter = Optional.presentIfNotNull(sceneFilter)
            )
        ).execute()
        
        return response.data?.findScenes?.scenes?.map {
            val title = if (!it.title.isNullOrBlank()) {
                it.title
            } else {
                it.files.firstOrNull()?.basename?.ifBlank { null } ?: "No Title"
            }
            Scene(
                id = it.id,
                videoUri = it.paths.stream.toFullUrl(),
                previewUri = it.paths.preview.toFullUrl(),
                subtitleUri = null,
                posterUri = it.paths.screenshot.toFullUrl(),
                name = title,
                description = it.details ?: "",
                studioName = it.studio?.name
            )
        } ?: emptyList()
    }

    suspend fun getScene(id: String): Scene? {
        val response = apolloClient.query(FindSceneQuery(id)).execute()
        return response.data?.findScene?.let {
            val title = if (!it.title.isNullOrBlank()) {
                it.title
            } else {
                it.files.firstOrNull()?.basename?.ifBlank { null } ?: "No Title"
            }
            Scene(
                id = it.id,
                videoUri = it.paths.stream.toFullUrl(),
                previewUri = it.paths.preview.toFullUrl(),
                subtitleUri = null,
                posterUri = it.paths.screenshot.toFullUrl(),
                name = title,
                description = it.details ?: "",
                studioName = it.studio?.name
            )
        }
    }

    suspend fun getSceneDetails(id: String): FindSceneQuery.FindScene? {
        val response = apolloClient.query(FindSceneQuery(id)).execute()
        if (response.hasErrors()) {
            println("GraphQL Errors for ID $id: ${response.errors}")
        }
        return response.data?.findScene
    }

    suspend fun recordScenePlay(sceneId: String) {
        withContext(Dispatchers.IO) {
            val payload = """{"query":"mutation AddScenePlay(${ '$' }id: ID!) { sceneAddPlay(id: ${ '$' }id) { count } }","variables":{"id":"${sceneId.escapeJson()}"}}"""
            val connection = URL("$baseUrl/graphql").openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(payload)
                }

                val responseCode = connection.responseCode
                val body = if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                } else {
                    connection.inputStream?.bufferedReader()?.use(BufferedReader::readText)
                }

                if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST || body?.contains("\"errors\"") == true) {
                    println("GraphQL Errors while recording play for scene $sceneId: HTTP $responseCode ${body.orEmpty()}")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun addFavorite(sceneId: String) {
        addTagToScene(sceneId, "Favorite")
        withContext(Dispatchers.IO) {
            val ratingPayload = """
                {
                  "query": "mutation SceneUpdate(${'$'}input: SceneUpdateInput!) { sceneUpdate(input: ${'$'}input) { id rating100 } }",
                  "variables": {
                    "input": {
                      "id": "${sceneId.escapeJson()}",
                      "rating100": 100
                    }
                  }
                }
            """.trimIndent()
            executeRawGraphQL(ratingPayload, "set rating 100 for scene $sceneId")
        }
    }

    suspend fun addWatchLaterTag(sceneId: String) {
        addTagToScene(sceneId, "Watch Later")
    }

    private suspend fun addTagToScene(sceneId: String, tagName: String) {
        withContext(Dispatchers.IO) {
            val sceneDetails = getSceneDetails(sceneId)
            val existingTagIds = sceneDetails?.tags?.map { it.id }?.toMutableList() ?: mutableListOf()

            val allTags = getTags()
            var tagId = allTags.find { it.name.equals(tagName, ignoreCase = true) }?.id

            if (tagId == null) {
                val createTagPayload = """
                    {
                      "query": "mutation TagCreate(${'$'}input: TagCreateInput!) { tagCreate(input: ${'$'}input) { id name } }",
                      "variables": {
                        "input": {
                          "name": "${tagName.escapeJson()}"
                        }
                      }
                    }
                """.trimIndent()
                executeRawGraphQL(createTagPayload, "create tag $tagName")
                tagId = getTags().find { it.name.equals(tagName, ignoreCase = true) }?.id
            }

            if (tagId != null && !existingTagIds.contains(tagId)) {
                existingTagIds.add(tagId)
                val tagIdsJson = existingTagIds.joinToString(",") { "\"${it.escapeJson()}\"" }
                val updateScenePayload = """
                    {
                      "query": "mutation SceneUpdate(${'$'}input: SceneUpdateInput!) { sceneUpdate(input: ${'$'}input) { id } }",
                      "variables": {
                        "input": {
                          "id": "${sceneId.escapeJson()}",
                          "tag_ids": [$tagIdsJson]
                        }
                      }
                    }
                """.trimIndent()
                executeRawGraphQL(updateScenePayload, "add tag $tagName to scene $sceneId")
            }
        }
    }

    private fun executeRawGraphQL(payload: String, actionName: String): String? {
        val connection = URL("$baseUrl/graphql").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(payload)
            }

            val responseCode = connection.responseCode
            val body = if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
            } else {
                connection.inputStream?.bufferedReader()?.use(BufferedReader::readText)
            }

            if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST || body?.contains("\"errors\"") == true) {
                println("GraphQL Errors while executing $actionName: HTTP $responseCode ${body.orEmpty()}")
            } else {
                println("GraphQL Success executing $actionName: HTTP $responseCode ${body.orEmpty()}")
            }
            body
        } catch (e: Exception) {
            println("Exception executing $actionName: ${e.message}")
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun String.escapeJson(): String {
        return replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    suspend fun getTags(): List<SceneCategory> {
        val response = apolloClient.query(FindTagsQuery()).execute()
        return response.data?.findTags?.tags?.map {
            SceneCategory(
                id = it.id,
                name = it.name
            )
        } ?: emptyList()
    }

    suspend fun getPerformers(): List<SceneCast> {
        val response = apolloClient.query(FindPerformersQuery()).execute()
        return response.data?.findPerformers?.performers?.map {
            SceneCast(
                id = it.id,
                characterName = it.name,
                realName = it.name,
                avatarUrl = it.image_path.toFullUrl()
            )
        } ?: emptyList()
    }

    suspend fun getStudios(): List<Pair<String, String>> {
        val filter = FindFilterType(per_page = Optional.present(-1))
        val response = apolloClient.query(FindStudiosQuery(filter = Optional.present(filter))).execute()
        return response.data?.findStudios?.studios?.map {
            it.id to it.name
        } ?: emptyList()
    }

    suspend fun getUIConfig(): Map<String, Any?>? {
        val response = apolloClient.query(GetUIConfigQuery()).execute()
        return response.data?.configuration?.ui as? Map<String, Any?>
    }

    suspend fun getSavedFilter(id: String): GetSavedFilterQuery.FindSavedFilter? {
        val response = apolloClient.query(GetSavedFilterQuery(id)).execute()
        return response.data?.findSavedFilter
    }

    suspend fun saveSceneActivity(sceneId: String, resumeTime: Double) {
        apolloClient.mutation(SaveSceneActivityMutation(sceneId, Optional.present(resumeTime))).execute()
    }

    suspend fun globalSearch(query: String, limit: Int = 5): SearchResult {
        val filter = FindFilterType(per_page = Optional.present(limit))
        val qFilter = FindFilterType(q = Optional.present(query), per_page = Optional.present(limit))
        val criterion = StringCriterionInput(
            value = query,
            modifier = com.kb2.stashtvapp.type.CriterionModifier.INCLUDES
        )

        val scenesResponse = apolloClient.query(
            SearchScenesQuery(
                filter = Optional.present(qFilter)
            )
        ).execute()

        val performersResponse = apolloClient.query(
            SearchPerformersQuery(
                performer_filter = Optional.present(PerformerFilterType(name = Optional.present(criterion))),
                filter = Optional.present(filter)
            )
        ).execute()

        val tagsResponse = apolloClient.query(
            SearchTagsQuery(
                tag_filter = Optional.present(TagFilterType(name = Optional.present(criterion))),
                filter = Optional.present(filter)
            )
        ).execute()

        val groupsResponse = apolloClient.query(
            SearchGroupsQuery(
                group_filter = Optional.present(GroupFilterType(name = Optional.present(criterion))),
                filter = Optional.present(filter)
            )
        ).execute()

        val markersResponse = apolloClient.query(
            SearchMarkersQuery(
                filter = Optional.present(qFilter)
            )
        ).execute()

        return SearchResult(
            scenes = scenesResponse.data?.findScenes?.scenes?.map {
                Scene(
                    id = it.id,
                    videoUri = it.paths.stream.toFullUrl(),
                    subtitleUri = null,
                    posterUri = it.paths.screenshot.toFullUrl(),
                    name = it.title ?: "No Title",
                    description = ""
                )
            } ?: emptyList(),
            performers = performersResponse.data?.findPerformers?.performers?.map {
                SceneCast(
                    id = it.id,
                    characterName = it.name,
                    realName = it.name,
                    avatarUrl = it.image_path.toFullUrl()
                )
            } ?: emptyList(),
            tags = tagsResponse.data?.findTags?.tags?.map {
                SceneCategory(id = it.id, name = it.name)
            } ?: emptyList(),
            groups = groupsResponse.data?.findGroups?.groups?.map {
                SceneGroup(id = it.id, name = it.name, posterUri = it.front_image_path.toFullUrl())
            } ?: emptyList(),
            markers = markersResponse.data?.findSceneMarkers?.scene_markers?.map {
                SceneMarker(
                    id = it.id,
                    title = it.title,
                    streamUri = it.stream.toFullUrl(),
                    previewUri = it.preview.toFullUrl(),
                    sceneId = it.scene.id,
                    sceneTitle = it.scene.title ?: ""
                )
            } ?: emptyList()
        )
    }
}
