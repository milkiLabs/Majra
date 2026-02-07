/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nav3recipes.migration.atomic.begin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.example.nav3recipes.migration.content.ScreenA
import com.example.nav3recipes.migration.content.ScreenA1
import com.example.nav3recipes.migration.content.ScreenB
import com.example.nav3recipes.migration.content.ScreenB1
import com.example.nav3recipes.migration.content.ScreenC
import com.example.nav3recipes.ui.setEdgeToEdgeConfig
import kotlin.reflect.KClass

/**
 * Basic Navigation2 example with the following navigation graph:
 *
 * A -> A, A1
 * B -> B, B1
 * C -> C
 * D
 *
 * - The starting destination (or home screen) is A.
 * - A, B and C are top level destinations that appear in a navigation bar.
 * - D is a dialog destination.
  * - Navigating to a top level destination pops all other top level destinations off the stack,
 * except for the start destination.
 * - Navigating back from the start destination exits the app.
 *
 * This will be the starting point for migration to Navigation 3.
 *
 * @see `AtomicMigrationTest` for instrumented tests that verify this behavior.
 */
class BeginAtomicMigrationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val currentBackStackEntry by navController.currentBackStackEntryAsState()

            Scaffold(bottomBar = {
                NavigationBar {
                    TOP_LEVEL_ROUTES.forEach { (key, value) ->
                        val isSelected = currentBackStackEntry?.destination.isRouteInHierarchy(key::class)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(key, navOptions {
                                    popUpTo(route = RouteA)
                                })
                            },
                            icon = {
                                Icon(
                                    imageVector = value.icon,
                                    contentDescription = value.description
                                )
                            },
                            label = { Text(value.description) }
                        )
                    }
                }
            })

            { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = BaseRouteA,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    featureASection(
                        onSubRouteClick = { navController.navigate(RouteA1) },
                        onDialogClick = { navController.navigate(RouteD) },
                    )
                    featureBSection(
                        onDetailClick = { id -> navController.navigate(RouteB1(id)) },
                        onDialogClick = { navController.navigate(RouteD) },
                    )
                    featureCSection(
                        onDialogClick = { navController.navigate(RouteD) },
                    )
                    dialog<RouteD> { key ->
                        Text(modifier = Modifier.background(Color.White), text = "Route D title (dialog)")
                    }
                }
            }
        }
    }
}

// Feature module A
private fun NavGraphBuilder.featureASection(
    onSubRouteClick: () -> Unit,
    onDialogClick: () -> Unit
) {
    navigation<BaseRouteA>(startDestination = RouteA) {
        composable<RouteA> { ScreenA(onSubRouteClick, onDialogClick) }
        composable<RouteA1> { ScreenA1() }
    }
}


// Feature module B
private fun NavGraphBuilder.featureBSection(
    onDetailClick: (id: String) -> Unit,
    onDialogClick: () -> Unit
) {
    navigation<BaseRouteB>(startDestination = RouteB) {
        composable<RouteB> { ScreenB(onDetailClick, onDialogClick) }
        composable<RouteB1> { key -> ScreenB1(id = key.toRoute<RouteB1>().id) }
    }
}

// Feature module C
private fun NavGraphBuilder.featureCSection(
    onDialogClick: () -> Unit
) {
    navigation<BaseRouteC>(startDestination = RouteC) {
        composable<RouteC> { ScreenC(onDialogClick) }
    }
}


private fun NavDestination?.isRouteInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } ?: false