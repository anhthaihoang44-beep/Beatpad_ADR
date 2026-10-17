package com.beatpad.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoute(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Packs : NavRoute("packs", "Packs", Icons.Default.GridView)
    data object Play : NavRoute("play", "Play", Icons.Default.PlayCircle)
    data object Learn : NavRoute("learn", "Learn", Icons.Default.School)
    data object Tracks : NavRoute("tracks", "My Tracks", Icons.Default.LibraryMusic)
    data object Loop : NavRoute("loop", "Loop Editor", Icons.Default.PlayCircle)
    data object LessonArena : NavRoute("lesson_arena", "Lesson Arena", Icons.Default.School)
    data object Settings : NavRoute("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems: List<NavRoute>
            get() = listOf(Packs, Play, Learn, Tracks)
    }
}
