package com.beatpad.app.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.beatpad.app.data.model.Lesson
import com.beatpad.app.data.model.SoundPack
import com.beatpad.app.data.repository.AssetLessonRepository
import com.beatpad.app.ui.learn.LearnScreen
import com.beatpad.app.ui.learn.LearnViewModel
import com.beatpad.app.ui.learn.LessonArenaScreen
import com.beatpad.app.ui.learn.LessonArenaViewModel
import com.beatpad.app.ui.library.LibraryScreen
import com.beatpad.app.ui.library.LibraryViewModel
import com.beatpad.app.ui.loop.LoopEditorScreen
import com.beatpad.app.ui.loop.LoopEditorViewModel
import com.beatpad.app.ui.navigation.BottomNavBar
import com.beatpad.app.ui.navigation.NavRoute
import com.beatpad.app.ui.pads.PadScreen
import com.beatpad.app.ui.pads.PadViewModel
import com.beatpad.app.ui.settings.SettingsScreen
import com.beatpad.app.ui.settings.SettingsViewModel
import com.beatpad.app.ui.theme.AmberAccent
import com.beatpad.app.ui.theme.BackgroundDark
import com.beatpad.app.ui.tracks.TracksScreen
import com.beatpad.app.ui.tracks.TracksViewModel

@Composable
fun BeatPadApp(
    padViewModel: PadViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoute.Play.route

    val context = LocalContext.current
    val app = context.applicationContext as Application

    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(app))
    val tracksViewModel: TracksViewModel = viewModel(factory = TracksViewModel.Factory(app))
    val learnViewModel: LearnViewModel = viewModel(factory = LearnViewModel.Factory(app))
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app, audioEngine = padViewModel.engine)
    )

    val padUiState by padViewModel.uiState.collectAsStateWithLifecycle()

    var activeLessonId by remember { mutableStateOf<String?>(null) }
    var activeLessonPackId by remember { mutableStateOf<String?>(null) }

    val shouldShowBottomNav = currentRoute in listOf(
        NavRoute.Packs.route,
        NavRoute.Play.route,
        NavRoute.Learn.route,
        NavRoute.Tracks.route
    )

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            if (shouldShowBottomNav) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(NavRoute.Play.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Play.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Play Screen
            composable(NavRoute.Play.route) {
                PadScreen(
                    viewModel = padViewModel,
                    onBack = {
                        navController.navigate(NavRoute.Packs.route) { launchSingleTop = true }
                    },
                    onOpenLoopEditor = {
                        navController.navigate(NavRoute.Loop.route)
                    },
                    onOpenTutorial = { packId ->
                        val filter = when {
                            packId.contains("trap", ignoreCase = true) -> "TRAP"
                            packId.contains("house", ignoreCase = true) -> "HOUSE"
                            else -> "ALL"
                        }
                        learnViewModel.setFilter(filter)
                        navController.navigate(NavRoute.Learn.route) {
                            popUpTo(NavRoute.Play.route) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenSettings = {
                        navController.navigate(NavRoute.Settings.route)
                    }
                )
            }

            // 2. Packs Library
            composable(NavRoute.Packs.route) {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onPackSelected = { packId ->
                        padViewModel.loadPack(packId)
                        navController.navigate(NavRoute.Play.route) {
                            popUpTo(NavRoute.Play.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 3. Learn Mode
            composable(NavRoute.Learn.route) {
                LearnScreen(
                    viewModel = learnViewModel,
                    onLessonSelected = { lessonId, packId ->
                        activeLessonId = lessonId
                        activeLessonPackId = packId
                        navController.navigate(NavRoute.LessonArena.route)
                    }
                )
            }

            // 4. My Tracks
            composable(NavRoute.Tracks.route) {
                TracksScreen(viewModel = tracksViewModel)
            }

            // 5. Loop Editor
            composable(NavRoute.Loop.route) {
                val pack = padUiState.pack
                if (pack != null) {
                    val loopViewModel: LoopEditorViewModel = viewModel(
                        key = "loop_${pack.id}",
                        factory = LoopEditorViewModel.Factory(
                            application = app,
                            pack = pack,
                            audioEngine = padViewModel.engine
                        )
                    )
                    LoopEditorScreen(
                        viewModel = loopViewModel,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // 6. Lesson Arena
            composable(NavRoute.LessonArena.route) {
                val lessonId = activeLessonId
                val packId = activeLessonPackId

                if (lessonId != null && packId != null) {
                    var lessonObj by remember(lessonId) { mutableStateOf<Lesson?>(null) }
                    var packObj by remember(packId) { mutableStateOf<SoundPack?>(null) }

                    LaunchedEffect(lessonId, packId) {
                        val lessonRepo = AssetLessonRepository(app)
                        lessonObj = lessonRepo.getLesson(lessonId)
                        val packResult = padViewModel.repository.getPack(packId)
                        val foundPack = packResult.getOrNull()
                        if (foundPack != null) {
                            packObj = foundPack
                            padViewModel.engine.preload(foundPack)
                        }
                    }

                    val currentLesson = lessonObj
                    val currentPack = packObj

                    if (currentLesson != null && currentPack != null) {
                        val arenaViewModel: LessonArenaViewModel = viewModel(
                            key = "arena_${currentLesson.id}",
                            factory = LessonArenaViewModel.Factory(
                                application = app,
                                lesson = currentLesson,
                                pack = currentPack,
                                audioEngine = padViewModel.engine
                            )
                        )
                        LessonArenaScreen(
                            viewModel = arenaViewModel,
                            onBack = {
                                learnViewModel.loadLessons()
                                navController.popBackStack()
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(BackgroundDark),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AmberAccent, modifier = Modifier.padding(24.dp))
                        }
                    }
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // 7. Settings Screen
            composable(NavRoute.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
