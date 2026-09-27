package com.calisvision.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.calisvision.data.AppContainer
import com.calisvision.domain.knowledge.ExerciseCatalog
import com.calisvision.ui.analysis.AnalysisScreen
import com.calisvision.ui.analysis.AnalysisViewModel
import com.calisvision.ui.analysis.queryDisplayName
import com.calisvision.ui.guide.ShootingGuideScreen
import com.calisvision.ui.home.HomeScreen
import com.calisvision.ui.result.ResultScreen
import com.calisvision.ui.result.ResultViewModel
import com.calisvision.ui.settings.SettingsScreen
import com.calisvision.ui.settings.SettingsViewModel

/** String routes; arguments are URL-encoded where they can contain reserved characters. */
object Routes {
    const val HOME = "home"
    const val GUIDE = "guide/{exerciseId}"
    const val ANALYSIS = "analysis/{exerciseId}?uri={uri}"
    const val RESULT = "result/{sessionId}"
    const val SETTINGS = "settings"

    fun guide(exerciseId: String) = "guide/$exerciseId"

    fun analysis(exerciseId: String, uri: Uri) = "analysis/$exerciseId?uri=${Uri.encode(uri.toString())}"

    fun result(sessionId: String) = "result/$sessionId"
}

@Composable
fun CalisNavHost(container: AppContainer) {
    val nav = rememberNavController()
    fun goHome() = nav.popBackStack(Routes.HOME, inclusive = false)

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onExerciseSelected = { nav.navigate(Routes.guide(it)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.GUIDE, arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })) { entry ->
            val exercise = ExerciseCatalog.byId(entry.arguments?.getString("exerciseId").orEmpty())
            if (exercise == null) {
                LaunchedEffect(Unit) { goHome() }
                return@composable
            }
            ShootingGuideScreen(
                exercise = exercise,
                pickVideo = container.pickVideo,
                onVideoPicked = { nav.navigate(Routes.analysis(exercise.id, it)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.ANALYSIS,
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.StringType },
                navArgument("uri") { type = NavType.StringType },
            ),
        ) { entry ->
            val exercise = ExerciseCatalog.byId(entry.arguments?.getString("exerciseId").orEmpty())
            val uri = entry.arguments?.getString("uri")?.let(Uri::parse)
            if (exercise == null || uri == null) {
                LaunchedEffect(Unit) { goHome() }
                return@composable
            }
            val resolver = LocalContext.current.contentResolver
            AnalysisScreen(
                viewModel = viewModel {
                    AnalysisViewModel(container.analyzer, container.sessions, uri, exercise, displayName = { queryDisplayName(resolver, uri) })
                },
                onCompleted = { sessionId ->
                    nav.navigate(Routes.result(sessionId)) { popUpTo(Routes.ANALYSIS) { inclusive = true } }
                },
                onExit = { goHome() },
            )
        }
        composable(Routes.RESULT, arguments = listOf(navArgument("sessionId") { type = NavType.StringType })) { entry ->
            val session = container.sessions[entry.arguments?.getString("sessionId").orEmpty()]
            if (session == null) {
                // Process death drops in-memory results; there is nothing to restore.
                LaunchedEffect(Unit) { goHome() }
                return@composable
            }
            ResultScreen(
                viewModel = viewModel { ResultViewModel(session, container.thresholds.thresholds) },
                onBack = { nav.popBackStack() },
                onAnotherVideo = { nav.navigate(Routes.guide(session.exercise.id)) { popUpTo(Routes.HOME) } },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(viewModel = viewModel { SettingsViewModel(container.thresholds) }, onBack = { nav.popBackStack() })
        }
    }
}
