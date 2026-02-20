package com.orakull.casanostra

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform