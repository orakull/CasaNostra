package com.orakull.casanostra

actual fun getPlatform(): Platform = object : Platform {
    override val name: String = "Web/Wasm"
}
