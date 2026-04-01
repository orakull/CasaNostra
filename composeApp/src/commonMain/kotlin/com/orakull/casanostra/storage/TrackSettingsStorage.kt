package com.orakull.casanostra.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SavedTrackSettings(
    val volume: Float,
    val isMuted: Boolean,
    val isSolo: Boolean
)

object TrackSettingsStorage {
    private val json = Json { ignoreUnknownKeys = true }

    private fun key(projectId: String) = "casanostra_track_settings_$projectId"

    fun save(projectId: String, settings: Map<String, SavedTrackSettings>) {
        LocalStorage.set(key(projectId), json.encodeToString(settings))
    }

    fun load(projectId: String): Map<String, SavedTrackSettings> {
        val raw = LocalStorage.get(key(projectId)) ?: return emptyMap()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun remove(projectId: String) {
        LocalStorage.remove(key(projectId))
    }
}
