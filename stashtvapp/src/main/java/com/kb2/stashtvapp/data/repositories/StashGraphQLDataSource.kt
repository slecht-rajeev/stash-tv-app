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
import com.kb2.stashtvapp.type.CriterionModifier
import com.kb2.stashtvapp.type.FindFilterType
import com.kb2.stashtvapp.type.GroupFilterType
import com.kb2.stashtvapp.type.HierarchicalMultiCriterionInput
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
        println("StashGraphQL: Executing getScenes(filter=$filter, sceneFilter=$sceneFilter)")
        val response = apolloClient.query(
            FindScenesQuery(
                filter = Optional.presentIfNotNull(filter),
                scene_filter = Optional.presentIfNotNull(sceneFilter)
            )
        ).execute()

        println("StashGraphQL: getScenes Response -> returned ${response.data?.findScenes?.scenes?.size ?: 0} scenes. Errors: ${response.errors}")
        
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
                studioName = it.studio?.name,
                resumeTime = it.resume_time ?: 0.0
            )
        } ?: emptyList()
    }

    suspend fun getScene(id: String): Scene? {
        println("StashGraphQL: Executing getScene(id=$id)")
        val response = apolloClient.query(FindSceneQuery(id)).execute()
        println("StashGraphQL: getScene($id) Response -> title='${response.data?.findScene?.title}'. Errors: ${response.errors}")
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
        println("StashGraphQL: Executing getSceneDetails(id=$id)")
        val response = apolloClient.query(FindSceneQuery(id)).execute()
        println("StashGraphQL: getSceneDetails($id) Response -> title='${response.data?.findScene?.title}'. Errors: ${response.errors}")
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

    suspend fun updateSceneTags(sceneId: String, tagIds: List<String>): Boolean {
        return withContext(Dispatchers.IO) {
            val tagIdsJson = tagIds.joinToString(",") { "\"${it.escapeJson()}\"" }
            val payload = """
                {
                  "query": "mutation SceneUpdate(${'$'}input: SceneUpdateInput!) { sceneUpdate(input: ${'$'}input) { id tags { id name } } }",
                  "variables": {
                    "input": {
                      "id": "${sceneId.escapeJson()}",
                      "tag_ids": [$tagIdsJson]
                    }
                  }
                }
            """.trimIndent()
            val response = executeRawGraphQL(payload, "updateSceneTags for scene $sceneId")
            response != null && !response.contains("\"errors\"")
        }
    }

    suspend fun getScenesForSavedFilter(
        filter: FindFilterType?,
        objectFilter: Any?
    ): List<Scene> {
        val convertedSceneFilter = convertObjectFilterToSceneFilter(objectFilter)
        println("StashGraphQL: getScenesForSavedFilter - raw objectFilter = $objectFilter")
        println("StashGraphQL: getScenesForSavedFilter - converted sceneFilter = $convertedSceneFilter")

        return withContext(Dispatchers.IO) {
            val filterMap = mutableMapOf<String, Any?>()
            val qValue = (filter?.q as? Optional.Present)?.value
            val sortValue = (filter?.sort as? Optional.Present)?.value
            val directionValue = (filter?.direction as? Optional.Present)?.value?.name

            if (qValue != null) filterMap["q"] = qValue
            if (sortValue != null) filterMap["sort"] = sortValue
            if (directionValue != null) filterMap["direction"] = directionValue
            filterMap["per_page"] = 200

            val variablesMap = mutableMapOf<String, Any?>()
            variablesMap["filter"] = filterMap
            if (convertedSceneFilter != null) {
                variablesMap["scene_filter"] = convertedSceneFilter
            }

            val query = """
                query FindScenes(${'$'}filter: FindFilterType, ${'$'}scene_filter: SceneFilterType) {
                  findScenes(filter: ${'$'}filter, scene_filter: ${'$'}scene_filter) {
                    scenes {
                      id
                      title
                      details
                      date
                      resume_time
                      paths {
                        screenshot
                        preview
                        stream
                      }
                      files {
                        basename
                        path
                      }
                      studio {
                        id
                        name
                        image_path
                      }
                      tags {
                        id
                        name
                      }
                      performers {
                        id
                        name
                        image_path
                      }
                    }
                  }
                }
            """.trimIndent()

            val payloadMap = mapOf(
                "query" to query,
                "variables" to variablesMap
            )

            val jsonPayload = payloadMap.toJsonElement()
            println("StashGraphQL: Executing getScenesForSavedFilter payload =\n$jsonPayload")
            val responseBody = executeRawGraphQL(jsonPayload, "getScenesForSavedFilter")
            println("StashGraphQL: getScenesForSavedFilter response =\n$responseBody")

            val scenes = parseScenesResponse(responseBody)
            println("StashGraphQL: getScenesForSavedFilter parsed ${scenes.size} scenes")
            if (scenes.isEmpty() && convertedSceneFilter != null) {
                println("StashGraphQL: SavedFilter with scene_filter returned 0 scenes, executing fallback getScenes(filter)...")
                return@withContext getScenes(filter = filter)
            }
            scenes
        }
    }

    fun convertObjectFilterToSceneFilter(objectFilter: Any?): Map<String, Any?>? {
        if (objectFilter == null) return null

        val filterObj: org.json.JSONObject = when (objectFilter) {
            is org.json.JSONObject -> objectFilter
            is String -> {
                try { org.json.JSONObject(objectFilter) } catch (e: Exception) { return null }
            }
            is Map<*, *> -> {
                try { org.json.JSONObject(objectFilter.toJsonElement()) } catch (e: Exception) { return null }
            }
            else -> return null
        }

        val result = mutableMapOf<String, Any?>()

        val multiCriteriaKeys = setOf("performers", "tags", "studios", "groups", "galleries", "performer_tags")
        val booleanFlagKeys = setOf("organized", "interactive", "performer_favorite")
        val stringFlagKeys = setOf("has_markers", "is_missing")

        val keys = filterObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val item = filterObj.opt(key) ?: continue

            if (booleanFlagKeys.contains(key)) {
                val boolVal = when (item) {
                    is Boolean -> item
                    is org.json.JSONObject -> {
                        val v = item.opt("value")
                        if (v is Boolean) v else v?.toString()?.toBoolean() ?: false
                    }
                    else -> item.toString().toBoolean()
                }
                result[key] = boolVal
            } else if (stringFlagKeys.contains(key)) {
                val strVal = when (item) {
                    is String -> item
                    is org.json.JSONObject -> item.optString("value", item.toString())
                    else -> item.toString()
                }
                result[key] = strVal
            } else if (multiCriteriaKeys.contains(key)) {
                val criterionObj = item as? org.json.JSONObject ?: continue
                var modifier = criterionObj.optString("modifier", "INCLUDES")

                val valueObj = criterionObj.opt("value")
                val itemsList = mutableListOf<String>()
                val excludedList = mutableListOf<String>()

                if (valueObj is org.json.JSONObject) {
                    val itemsArr = valueObj.optJSONArray("items")
                    if (itemsArr != null) {
                        for (i in 0 until itemsArr.length()) {
                            val itm = itemsArr.optJSONObject(i)
                            val id = itm?.optString("id") ?: itemsArr.optString(i)
                            if (!id.isNullOrBlank()) itemsList.add(id)
                        }
                    }
                    val excludedArr = valueObj.optJSONArray("excluded")
                    if (excludedArr != null) {
                        for (i in 0 until excludedArr.length()) {
                            val itm = excludedArr.optJSONObject(i)
                            val id = itm?.optString("id") ?: excludedArr.optString(i)
                            if (!id.isNullOrBlank()) excludedList.add(id)
                        }
                    }
                } else if (valueObj is org.json.JSONArray) {
                    for (i in 0 until valueObj.length()) {
                        val itm = valueObj.optJSONObject(i)
                        val id = itm?.optString("id") ?: valueObj.optString(i)
                        if (!id.isNullOrBlank()) itemsList.add(id)
                    }
                } else if (valueObj != null && valueObj != org.json.JSONObject.NULL) {
                    itemsList.add(valueObj.toString())
                }

                if (modifier == "EXCLUDES") {
                    modifier = "INCLUDES"
                    excludedList.addAll(itemsList)
                    itemsList.clear()
                }

                val criterionMap = mutableMapOf<String, Any?>("modifier" to modifier)
                if (modifier != "IS_NULL" && modifier != "NOT_NULL") {
                    criterionMap["value"] = itemsList
                    if (excludedList.isNotEmpty()) {
                        criterionMap["excludes"] = excludedList
                    }
                }

                if (criterionObj.has("depth")) {
                    criterionMap["depth"] = criterionObj.optInt("depth")
                }

                result[key] = criterionMap
            } else if (item is org.json.JSONObject) {
                val modifier = item.optString("modifier", "EQUALS")
                val criterionMap = mutableMapOf<String, Any?>("modifier" to modifier)

                if (modifier != "IS_NULL" && modifier != "NOT_NULL") {
                    val rawVal = item.opt("value")
                    if (rawVal is org.json.JSONObject) {
                        if (rawVal.has("value")) criterionMap["value"] = rawVal.get("value")
                        if (rawVal.has("value2")) criterionMap["value2"] = rawVal.get("value2")
                    } else if (rawVal != null && rawVal != org.json.JSONObject.NULL) {
                        criterionMap["value"] = rawVal
                        if (item.has("value2")) {
                            criterionMap["value2"] = item.get("value2")
                        }
                    }
                }

                result[key] = criterionMap
            }
        }

        return result
    }

    private fun parseScenesResponse(jsonResponse: String?): List<Scene> {
        if (jsonResponse.isNullOrBlank()) return emptyList()
        return try {
            val jsonObject = org.json.JSONObject(jsonResponse)
            val dataObj = jsonObject.optJSONObject("data")
            val findScenesObj = dataObj?.optJSONObject("findScenes")
            val scenesArray = findScenesObj?.optJSONArray("scenes") ?: return emptyList()

            val list = mutableListOf<Scene>()
            for (i in 0 until scenesArray.length()) {
                val sceneObj = scenesArray.optJSONObject(i) ?: continue
                val id = sceneObj.optString("id", "")
                val titleRaw = sceneObj.optString("title", "")
                val details = sceneObj.optString("details", "")

                val pathsObj = sceneObj.optJSONObject("paths")
                val stream = pathsObj?.optString("stream", "")
                val preview = pathsObj?.optString("preview", "")
                val screenshot = pathsObj?.optString("screenshot", "")

                val studioObj = sceneObj.optJSONObject("studio")
                val studioName = if (studioObj != null && !studioObj.isNull("name")) studioObj.optString("name") else null

                var title = if (titleRaw.isNotBlank() && titleRaw != "null") titleRaw else ""
                if (title.isBlank()) {
                    val filesArray = sceneObj.optJSONArray("files")
                    if (filesArray != null && filesArray.length() > 0) {
                        val firstFile = filesArray.optJSONObject(0)
                        title = firstFile?.optString("basename", "") ?: ""
                    }
                }
                if (title.isBlank()) title = "No Title"

                list.add(
                    Scene(
                        id = id,
                        videoUri = stream.toFullUrl(),
                        previewUri = preview.toFullUrl(),
                        subtitleUri = null,
                        posterUri = screenshot.toFullUrl(),
                        name = title,
                        description = details,
                        studioName = studioName
                    )
                )
            }
            list
        } catch (e: Exception) {
            println("StashGraphQL: Exception parsing scenes response: ${e.message}")
            emptyList()
        }
    }

    private fun Any?.toJsonElement(): String {
        return when (this) {
            null -> "null"
            is Boolean -> toString()
            is Number -> toString()
            is String -> "\"${escapeJson()}\""
            is org.json.JSONObject -> toString()
            is org.json.JSONArray -> toString()
            is Map<*, *> -> {
                val entries = entries.mapNotNull { (k, v) ->
                    if (k != null) "\"${k.toString().escapeJson()}\":${v.toJsonElement()}" else null
                }
                "{${entries.joinToString(",")}}"
            }
            is Iterable<*> -> {
                val items = map { it.toJsonElement() }
                "[${items.joinToString(",")}]"
            }
            else -> "\"${toString().escapeJson()}\""
        }
    }

    private fun String.escapeJson(): String {
        return replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    suspend fun getWatchLaterScenes(): List<Scene> {
        val allTags = getTags()
        val watchLaterTag = allTags.find {
            it.name.equals("watch later", ignoreCase = true) ||
            it.name.equals("watchlater", ignoreCase = true) ||
            it.name.replace(" ", "").equals("watchlater", ignoreCase = true)
        }

        println("StashWatchLater: Found tag = ${watchLaterTag?.name} (id=${watchLaterTag?.id}), total tags in Stash = ${allTags.size}")

        val findFilter = FindFilterType(per_page = Optional.present(200))

        if (watchLaterTag != null) {
            val sceneFilter = SceneFilterType(
                tags = Optional.present(
                    HierarchicalMultiCriterionInput(
                        value = Optional.present(listOf(watchLaterTag.id)),
                        modifier = CriterionModifier.INCLUDES
                    )
                )
            )
            val scenes = getScenes(filter = findFilter, sceneFilter = sceneFilter)
            println("StashWatchLater: Fetched ${scenes.size} scenes for tag '${watchLaterTag.name}'")
            return scenes
        }

        println("StashWatchLater: 'Watch Later' tag not found in Stash DB.")
        return emptyList()
    }

    suspend fun getFavouriteScenes(): List<Scene> {
        val allTags = getTags()
        val favoriteTag = allTags.find {
            it.name.equals("favorite", ignoreCase = true) ||
            it.name.equals("favorites", ignoreCase = true) ||
            it.name.equals("favourite", ignoreCase = true) ||
            it.name.equals("favourites", ignoreCase = true)
        }

        val findFilter = FindFilterType(per_page = Optional.present(200))

        return if (favoriteTag != null) {
            val sceneFilter = SceneFilterType(
                tags = Optional.present(
                    HierarchicalMultiCriterionInput(
                        value = Optional.present(listOf(favoriteTag.id)),
                        modifier = CriterionModifier.INCLUDES
                    )
                )
            )
            val scenes = getScenes(filter = findFilter, sceneFilter = sceneFilter)
            println("StashFavorites: Fetched ${scenes.size} scenes for tag '${favoriteTag.name}'")
            scenes
        } else {
            val scenes = getScenes(filter = findFilter)
            println("StashFavorites: Fetched ${scenes.size} scenes")
            scenes
        }
    }

    suspend fun getTags(): List<SceneCategory> {
        val filter = FindFilterType(per_page = Optional.present(-1))
        println("StashGraphQL: Executing getTags(filter=$filter)")
        val response = apolloClient.query(FindTagsQuery(filter = Optional.present(filter))).execute()
        println("StashGraphQL: getTags Response -> returned ${response.data?.findTags?.tags?.size ?: 0} tags. Errors: ${response.errors}")
        return response.data?.findTags?.tags?.map { tag ->
            SceneCategory(
                id = tag.id,
                name = tag.name,
                imageUri = tag.image_path.toFullUrl(),
                subTags = tag.children.map { child ->
                    SceneCategory(
                        id = child.id,
                        name = child.name,
                        imageUri = child.image_path.toFullUrl()
                    )
                }
            )
        } ?: emptyList()
    }

    suspend fun getPositionsSubTags(): List<SceneCategory> {
        val allTags = getTags()
        val positionsTag = allTags.find { it.name.equals("Positions", ignoreCase = true) }

        var subTags = positionsTag?.subTags ?: emptyList()
        if (subTags.isEmpty() && positionsTag != null) {
            val tagFilter = TagFilterType(
                parents = Optional.present(
                    HierarchicalMultiCriterionInput(
                        value = Optional.present(listOf(positionsTag.id)),
                        modifier = CriterionModifier.INCLUDES
                    )
                )
            )
            val filter = FindFilterType(per_page = Optional.present(-1))
            println("StashGraphQL: Querying sub-tags of Positions via tag_filter parents")
            val response = apolloClient.query(
                FindTagsQuery(
                    filter = Optional.present(filter),
                    tag_filter = Optional.present(tagFilter)
                )
            ).execute()

            subTags = response.data?.findTags?.tags?.map {
                SceneCategory(
                    id = it.id,
                    name = it.name,
                    imageUri = it.image_path.toFullUrl()
                )
            } ?: emptyList()
        }

        println("StashPositions: Total sub-tags found for 'Positions' = ${subTags.size}")
        return subTags
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
        println("StashGraphQL: Executing getStudios(filter=$filter)")
        val response = apolloClient.query(FindStudiosQuery(filter = Optional.present(filter))).execute()
        println("StashGraphQL: getStudios Response -> returned ${response.data?.findStudios?.studios?.size ?: 0} studios. Errors: ${response.errors}")
        return response.data?.findStudios?.studios?.map {
            it.id to it.name
        } ?: emptyList()
    }

    suspend fun getUIConfig(): Map<String, Any?>? {
        println("StashGraphQL: Executing getUIConfig()")
        val response = apolloClient.query(GetUIConfigQuery()).execute()
        println("StashGraphQL: getUIConfig Response -> ${response.data?.configuration?.ui}. Errors: ${response.errors}")
        return response.data?.configuration?.ui as? Map<String, Any?>
    }

    suspend fun getSavedFilter(id: String): GetSavedFilterQuery.FindSavedFilter? {
        println("StashGraphQL: Executing getSavedFilter(id=$id)")
        val response = apolloClient.query(GetSavedFilterQuery(id)).execute()
        println("StashGraphQL: getSavedFilter($id) Response -> name='${response.data?.findSavedFilter?.name}', find_filter=${response.data?.findSavedFilter?.find_filter}. Errors: ${response.errors}")
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
