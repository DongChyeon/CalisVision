package com.calisvision.ui.navigation

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.calisvision.data.AppContainer
import com.calisvision.domain.knowledge.ExerciseCatalog
import com.calisvision.ui.guide.ShootingGuideScreen
import com.calisvision.ui.home.HomeScreen

/** String routes; arguments are URL-encoded where they can contain reserved characters. */
object Routes {
    const val HOME = "home"
    const val GUIDE = "guide/{exerciseId}"
    const val ANALYSIS = "analysis/{exerciseId}?uri={uri}"

    fun guide(exerciseId: String) = "guide/$exerciseId"

    fun analysis(exerciseId: String, uri: Uri) = "analysis/$exerciseId?uri=${Uri.encode(uri.toString())}"
}

@Composable
fun CalisNavHost(container: AppContainer) {
    val nav = rememberNavController()
    fun goHome() = nav.popBackStack(Routes.HOME, inclusive = false)

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onExerciseSelected = { nav.navigate(Routes.guide(it)) })
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
            // Placeholder until the analysis screen lands (P3-2).
            Text(entry.arguments?.getString("uri").orEmpty())
        }
    }
}
