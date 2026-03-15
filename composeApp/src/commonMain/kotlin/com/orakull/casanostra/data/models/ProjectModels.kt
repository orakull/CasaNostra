package com.orakull.casanostra.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String = "", // UUID string
    val name: String,
    @SerialName("owner_id") val ownerId: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ProjectTrack(
    val id: String = "",
    @SerialName("project_id") val projectId: String,
    val name: String,
    @SerialName("file_path") val filePath: String,
    @SerialName("created_at") val createdAt: String = ""
)
