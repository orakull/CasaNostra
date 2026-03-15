package com.orakull.casanostra.ui

import androidx.compose.runtime.Composable

enum class ScreenOrientation {
    Portrait,
    Landscape,
    Unspecified
}

/**
 * Locks the screen orientation to the given value while the composable is in the composition.
 * Restores the previous orientation when the composable leaves the composition.
 */
@Composable
expect fun LockScreenOrientation(orientation: ScreenOrientation)
