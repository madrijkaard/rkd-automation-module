package com.rkd.module

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rkd.type.MenuType

object MenuModule {

    @Composable
    fun MenuScreen(onOptionSelected: (MenuType) -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onOptionSelected(MenuType.NEW_PROJECT) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("New Project")
                }

                Button(
                    onClick = { onOptionSelected(MenuType.OPEN_PROJECT) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Open Project")
                }

                Button(
                    onClick = { onOptionSelected(MenuType.COORDINATE) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Map Coordinates")
                }

                Button(
                    onClick = { onOptionSelected(MenuType.EXIT) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Quit")
                }
            }
        }
    }
}