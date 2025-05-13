package com.rkd

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import com.rkd.module.MapCoordinateModule
import com.rkd.module.MenuModule
import com.rkd.module.NewProjectModule
import com.rkd.module.OpenProjectModule
import com.rkd.type.MenuType
import com.rkd.type.MenuType.*

@Composable
fun App(exitApplication: () -> Unit) {

    MaterialTheme {

        var selectedOption by remember { mutableStateOf<MenuType?>(null) }

        when (selectedOption) {
            null -> MenuModule.MenuScreen { selectedOption = it }
            NEW_PROJECT -> NewProjectModule.createNewProject { selectedOption = null }
            OPEN_PROJECT -> OpenProjectModule.createOpenProject { selectedOption = null }
            COORDINATE -> MapCoordinateModule.createMapCoordinate { selectedOption = null }
            EXIT -> exitApplication()
        }
    }
}
