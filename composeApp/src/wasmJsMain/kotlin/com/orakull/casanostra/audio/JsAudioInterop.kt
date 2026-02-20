package com.orakull.casanostra.audio

// ─── AudioContext ────────────────────────────────────────────────────────────

internal external class AudioContext : JsAny {
    val currentTime: Double
    val destination: AudioDestinationNode
    fun createGain(): GainNode
    fun createBufferSource(): AudioBufferSourceNode
    fun resume(): JsAny // Promise<undefined>
}

internal external class AudioDestinationNode : JsAny

// ─── GainNode ────────────────────────────────────────────────────────────────

internal external class GainNode : JsAny {
    val gain: AudioParam
    fun connect(destination: JsAny): JsAny
}

internal external class AudioParam : JsAny {
    var value: Float
}

// ─── AudioBufferSourceNode ───────────────────────────────────────────────────

internal external class AudioBufferSourceNode : JsAny {
    var buffer: AudioBuffer?
    val playbackRate: AudioParam
    var loop: Boolean
    fun connect(destination: JsAny): JsAny
    fun start(offset: Double, offsetInBuffer: Double)
    fun stop()
}

// ─── AudioBuffer ─────────────────────────────────────────────────────────────

internal external class AudioBuffer : JsAny {
    val duration: Double
    val sampleRate: Float
    val numberOfChannels: Int
}

// ─── AudioContext constructor ────────────────────────────────────────────────

@JsFun("() => new AudioContext()")
internal external fun newAudioContext(): AudioContext

// ─── Decode audio: accepts Uint8Array (JsAny), not ByteArray ─────────────────

@JsFun("""
(ctx, uint8arr, onSuccess, onError) => {
    const copy = uint8arr.buffer.slice(uint8arr.byteOffset, uint8arr.byteOffset + uint8arr.byteLength);
    ctx.decodeAudioData(copy)
        .then(function(buf) { onSuccess(buf); })
        .catch(function(e) { onError(String(e)); });
}
""")
internal external fun decodeAudioDataWithCallbacks(
    ctx: AudioContext,
    data: JsAny, // Uint8Array
    onSuccess: (AudioBuffer) -> Unit,
    onError: (JsString) -> Unit
)

// ─── ByteArray → Uint8Array helper ──────────────────────────────────────────

@JsFun("(len) => new Uint8Array(len)")
internal external fun newUint8Array(len: Int): JsAny

@JsFun("(arr, idx, value) => { arr[idx] = value; }")
internal external fun uint8ArraySet(arr: JsAny, idx: Int, value: Int)

/**
 * Convert Kotlin ByteArray to a JS Uint8Array.
 * We cannot pass ByteArray directly to JS interop in Kotlin/Wasm,
 * so we copy byte-by-byte.
 */
internal fun byteArrayToUint8Array(bytes: ByteArray): JsAny {
    val arr = newUint8Array(bytes.size)
    for (i in bytes.indices) {
        uint8ArraySet(arr, i, bytes[i].toInt() and 0xFF)
    }
    return arr
}

// ─── console.error helper ────────────────────────────────────────────────────

@JsFun("(msg, err) => console.error(msg, err)")
internal external fun consoleError(msg: JsString, err: JsAny)
