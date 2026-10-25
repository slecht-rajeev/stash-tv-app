package com.kb2.stashtvapp.data.entities

data class Studio(
    val id: String,
    val name: String,
    val imageUri: String? = null,
    val sceneCount: Int = 0
)
