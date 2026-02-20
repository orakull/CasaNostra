package com.orakull.casanostra.audio

data class TrackInfo(
    val name: String,
    val resourceBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as TrackInfo
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}
