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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

// Feature module A
@Serializable data object BaseRouteA
@Serializable data object RouteA
@Serializable data object RouteA1

// Feature module B
@Serializable data object BaseRouteB
@Serializable data object RouteB
@Serializable data class RouteB1(val id: String)

// Feature module C
@Serializable data object BaseRouteC
@Serializable data object RouteC

// Common UI modules
@Serializable data object RouteD


val TOP_LEVEL_ROUTES = mapOf(
    BaseRouteA to NavBarItem(icon = Icons.Default.Home, description = "Route A"),
    BaseRouteB to NavBarItem(icon = Icons.Default.Face, description = "Route B"),
    BaseRouteC to NavBarItem(icon = Icons.Default.Camera, description = "Route C"),
)

class NavBarItem(
    val icon: ImageVector,
    val description: String
)