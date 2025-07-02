package com.yifeplayte.wommo.activity.pages

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.utils.SharedPreferences.get
import com.yifeplayte.wommo.utils.SharedPreferences.mSP
import com.yifeplayte.wommo.utils.SharedPreferences.put
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.VerticalDivider
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.getWindowSize

var pageRatio: Float get() = mSP.get("pageRatio", 0.5f); set(value) = mSP.put("pageRatio", value)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Main() {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember(navBackStackEntry) {
        derivedStateOf { navBackStackEntry?.destination?.route ?: "HomePage" }
    }

    MiuixTheme(
        colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Scaffold {
            BoxWithConstraints {
                if (isLandscape || maxWidth > 768.dp) {
                    LandscapeLayout(navController, currentRoute)
                } else {
                    PortraitLayout(navController, currentRoute)
                }
            }
        }
    }
}

@Composable
fun PortraitLayout(navController: NavHostController, currentRoute: String) {
    val getWindowSize by rememberUpdatedState(getWindowSize())
    val windowWidth = getWindowSize.width
    val easing = CubicBezierEasing(0.12f, 0.38f, 0.2f, 1f)

    NavHost(navController = navController, startDestination = "HomePage", enterTransition = {
        slideInHorizontally(
            initialOffsetX = { windowWidth },
            animationSpec = tween(durationMillis = 500, easing = easing)
        )
    }, exitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -windowWidth / 5 },
            animationSpec = tween(durationMillis = 500, easing = easing)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 500), targetAlpha = 0.5f
        )
    }, popEnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -windowWidth / 5 },
            animationSpec = tween(durationMillis = 500, easing = easing)
        ) + fadeIn(
            animationSpec = tween(durationMillis = 500), initialAlpha = 0.5f
        )
    }, popExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { windowWidth },
            animationSpec = tween(durationMillis = 500, easing = easing)
        )
    }) {
        composable("HomePage") { HomePage(navController, currentRoute) }
        pageDestinations(navController, currentRoute)
    }
}

@Composable
fun LandscapeLayout(navController: NavHostController, currentRoute: String) {
    val getWindowSize by rememberUpdatedState(getWindowSize())
    val windowWidth = getWindowSize.width
    var weight by remember { mutableFloatStateOf(pageRatio) }
    var potentialWeight by remember { mutableFloatStateOf(weight) }
    val dragState = rememberDraggableState {
        val nextPotentialWeight = potentialWeight + it / windowWidth
        potentialWeight = nextPotentialWeight
        weight = nextPotentialWeight.coerceIn(0.35f, 0.65f)
    }
    var finalWeight by remember { mutableFloatStateOf(weight) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Box(
            modifier = Modifier.weight(weight)
        ) {
            HomePage(navController, currentRoute)
        }
        VerticalDivider(
            modifier = Modifier
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        potentialWeight = weight
                    },
                    onDragStopped = {
                        finalWeight = weight
                        pageRatio = finalWeight
                    })
                .padding(horizontal = 12.dp)
        )
        NavHost(
            navController = navController,
            startDestination = "HomePage",
            modifier = Modifier.weight(1f - weight),
            enterTransition = { fadeIn(initialAlpha = 1f) },
            exitTransition = { fadeOut(targetAlpha = 1f) },
        ) {
            composable("HomePage") { EmptyPage() }
            pageDestinations(navController, currentRoute)
        }
    }
}

fun NavGraphBuilder.pageDestinations(
    navController: NavHostController, currentRoute: String
) { // composable("ChoosePage") { ChoosePage(navController) }
}

@Composable
fun EmptyPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_foreground),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.secondary,
            modifier = Modifier.size(300.dp)
        )
    }
}
