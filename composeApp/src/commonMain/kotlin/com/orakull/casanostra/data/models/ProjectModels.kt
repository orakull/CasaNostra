package com.orakull.casanostra.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Workspace(
    val id: String = "",
    val name: String,
    @SerialName("owner_id") val ownerId: String = "",
    @SerialName("share_token") val shareToken: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class WorkspaceMember(
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("joined_at") val joinedAt: String = ""
)

@Serializable
data class Project(
    val id: String = "",
    val name: String,
    @SerialName("owner_id") val ownerId: String = "",
    @SerialName("workspace_id") val workspaceId: String? = null,
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
