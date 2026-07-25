/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kb2.stashtvapp.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kb2.stashtvapp.presentation.screens.Screens
import com.kb2.stashtvapp.presentation.screens.categories.CategorySceneListScreen
import com.kb2.stashtvapp.presentation.screens.studios.StudioDetailsScreen
import com.kb2.stashtvapp.presentation.screens.dashboard.DashboardScreen
import com.kb2.stashtvapp.presentation.screens.scenes.SceneDetailsScreen
import com.kb2.stashtvapp.presentation.screens.videoPlayer.VideoPlayerScreen

@Composable
fun App(
    onBackPressed: () -> Unit
) {

    val navController = rememberNavController()
    var isComingBackFromDifferentScreen by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = Screens.Dashboard(),
        builder = {
            composable(
                route = Screens.CategorySceneList(),
                arguments = listOf(
                    navArgument(CategorySceneListScreen.CategoryIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) {
                CategorySceneListScreen(
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    },
                    onSceneSelected = { scene ->
                        navController.navigate(
                            Screens.SceneDetails.withArgs(scene.id)
                        )
                    }
                )
            }
            composable(
                route = Screens.StudioDetails(),
                arguments = listOf(
                    navArgument(StudioDetailsScreen.StudioIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) {
                StudioDetailsScreen(
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    },
                    onSceneSelected = { scene ->
                        navController.navigate(
                            Screens.SceneDetails.withArgs(scene.id)
                        )
                    }
                )
            }
            composable(
                route = Screens.SceneDetails(),
                arguments = listOf(
                    navArgument(SceneDetailsScreen.SceneIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val sceneId = backStackEntry.arguments?.getString(SceneDetailsScreen.SceneIdBundleKey)
                SceneDetailsScreen(
                    goToScenePlayer = {
                        if (sceneId != null) {
                            navController.navigate(Screens.VideoPlayer.withArgs(sceneId))
                        }
                    },
                    refreshScreenWithNewScene = { scene ->
                        navController.navigate(
                            Screens.SceneDetails.withArgs(scene.id)
                        ) {
                            popUpTo(Screens.SceneDetails()) {
                                inclusive = true
                            }
                        }
                    },
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    }
                )
            }
            composable(route = Screens.Dashboard()) {
                DashboardScreen(
                    openCategorySceneList = { categoryId ->
                        navController.navigate(
                            Screens.CategorySceneList.withArgs(categoryId)
                        )
                    },
                    openStudioDetails = { studioId ->
                        navController.navigate(
                            Screens.StudioDetails.withArgs(studioId)
                        )
                    },
                    openSceneDetailsScreen = { sceneId ->
                        navController.navigate(
                            Screens.SceneDetails.withArgs(sceneId)
                        )
                    },
                    openVideoPlayer = { scene ->
                        navController.navigate(Screens.VideoPlayer.withArgs(scene.id))
                    },
                    onBackPressed = onBackPressed,
                    isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
                    resetIsComingBackFromDifferentScreen = {
                        isComingBackFromDifferentScreen = false
                    }
                )
            }
            composable(
                route = Screens.VideoPlayer(),
                arguments = listOf(
                    navArgument(VideoPlayerScreen.SceneIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) {
                VideoPlayerScreen(
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    }
                )
            }
        }
    )
}
