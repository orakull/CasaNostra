package com.orakull.casanostra.data.repository

import com.orakull.casanostra.data.models.Workspace
import com.orakull.casanostra.data.models.WorkspaceMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WorkspaceRepository(
    private val supabaseClient: SupabaseClient
) {
    private val _workspaces = MutableStateFlow<List<Workspace>>(emptyList())
    val workspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    // Загружает воркспейсы где юзер — владелец или участник
    suspend fun fetchWorkspaces(userId: String) {
        val owned = supabaseClient.from("workspaces")
            .select {
                filter { eq("owner_id", userId) }
                order("created_at", order = Order.DESCENDING)
            }.decodeList<Workspace>()

        val joinedIds = supabaseClient.from("workspace_members")
            .select {
                filter { eq("user_id", userId) }
            }.decodeList<WorkspaceMember>()
            .map { it.workspaceId }

        val joined = if (joinedIds.isNotEmpty()) {
            supabaseClient.from("workspaces")
                .select {
                    filter { isIn("id", joinedIds) }
                    order("created_at", order = Order.DESCENDING)
                }.decodeList<Workspace>()
        } else emptyList()

        _workspaces.value = (owned + joined).distinctBy { it.id }
    }

    suspend fun createWorkspace(name: String, userId: String): Workspace {
        val created = supabaseClient.from("workspaces")
            .insert(Workspace(name = name, ownerId = userId)) {
                select()
            }.decodeSingle<Workspace>()

        _workspaces.update { listOf(created) + it }
        return created
    }

    suspend fun renameWorkspace(workspaceId: String, newName: String) {
        _workspaces.update { current ->
            current.map { if (it.id == workspaceId) it.copy(name = newName) else it }
        }
        supabaseClient.from("workspaces")
            .update({ set("name", newName) }) {
                filter { eq("id", workspaceId) }
            }
    }

    // Генерирует share_token и возвращает его
    @OptIn(ExperimentalUuidApi::class)
    suspend fun generateShareToken(workspaceId: String): String {
        val token = Uuid.random().toString()
        supabaseClient.from("workspaces")
            .update({ set("share_token", token) }) {
                filter { eq("id", workspaceId) }
            }
        _workspaces.update { current ->
            current.map { if (it.id == workspaceId) it.copy(shareToken = token) else it }
        }
        return token
    }

    // Находит воркспейс по share_token (для гостевого/join флоу)
    suspend fun findByShareToken(shareToken: String): Workspace? {
        return supabaseClient.from("workspaces")
            .select {
                filter { eq("share_token", shareToken) }
            }.decodeList<Workspace>()
            .firstOrNull()
    }

    // Авторизованный юзер вступает в воркспейс по share_token
    suspend fun joinWorkspace(workspaceId: String, userId: String) {
        val member = WorkspaceMember(workspaceId = workspaceId, userId = userId)
        supabaseClient.from("workspace_members").insert(member)

        // Подгружаем воркспейс в локальный список если его там нет
        val alreadyHave = _workspaces.value.any { it.id == workspaceId }
        if (!alreadyHave) {
            val workspace = supabaseClient.from("workspaces")
                .select { filter { eq("id", workspaceId) } }
                .decodeList<Workspace>()
                .firstOrNull()
            if (workspace != null) {
                _workspaces.update { it + workspace }
            }
        }
    }

    fun clearWorkspaces() {
        _workspaces.value = emptyList()
    }
}
