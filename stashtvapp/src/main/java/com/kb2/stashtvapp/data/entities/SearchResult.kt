package com.kb2.stashtvapp.data.entities

data class SearchResult(
    val scenes: List<Scene> = emptyList(),
    val performers: List<SceneCast> = emptyList(),
    val tags: List<SceneCategory> = emptyList(),
    val groups: List<SceneGroup> = emptyList(),
    val markers: List<SceneMarker> = emptyList()
)

data class SceneGroup(
    val id: String,
    val name: String,
    val posterUri: String
)

data class SceneMarker(
    val id: String,
    val title: String,
    val streamUri: String,
    val previewUri: String,
    val sceneId: String,
    val sceneTitle: String
)
