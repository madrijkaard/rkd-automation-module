package com.rkd.module

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.awt.MouseInfo
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame

object MapCoordinateModule {

    private val collectedCoordinates = mutableStateListOf<Pair<Int, Int>>()
    private var currentMouseCoordinates by mutableStateOf("Aguardando coordenadas...")
    private var monitorJob: Job? = null

    private lateinit var frame: JFrame

    fun startMonitoringCoordinates() {
        if (monitorJob?.isActive == true) return
        monitorJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val pointerLocation = MouseInfo.getPointerInfo().location
                currentMouseCoordinates = "Coordenadas atuais: x=${pointerLocation.x}, y=${pointerLocation.y}"
                delay(1000)
            }
        }
    }

    fun stopMonitoringCoordinates() {
        monitorJob?.cancel()
    }

    fun setupGlobalKeyListener() {

        frame = JFrame().apply {
            isUndecorated = true
            isFocusable = true
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            setSize(0, 0)
            addKeyListener(object : KeyListener {
                override fun keyTyped(e: KeyEvent?) {}
                override fun keyPressed(e: KeyEvent?) {
                    if (e?.keyCode == KeyEvent.VK_SPACE) {
                        val pointerLocation = MouseInfo.getPointerInfo().location
                        collectedCoordinates.add(pointerLocation.x to pointerLocation.y)
                    }
                }
                override fun keyReleased(e: KeyEvent?) {}
            })
            isVisible = true
        }
    }

    @Composable
    fun createMapCoordinate(onBack: () -> Unit) {
        LaunchedEffect(Unit) {
            startMonitoringCoordinates()
            setupGlobalKeyListener()
        }

        DisposableEffect(Unit) {
            onDispose {
                stopMonitoringCoordinates()
                frame.dispose()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(currentMouseCoordinates, Modifier.padding(16.dp))
            Text("Pressione 'Barra de Espaço' para capturar as coordenadas atuais do mouse.")

            Spacer(modifier = Modifier.height(16.dp))

            CoordinateTableWithScroll(collectedCoordinates)

            Button(
                onClick = {
                    collectedCoordinates.clear()
                    stopMonitoringCoordinates()
                    onBack()
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Back")
            }
        }
    }

    @Composable
    fun CoordinateTableWithScroll(coordinates: List<Pair<Int, Int>>) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Tabela de Coordenadas",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colors.background)
                    .padding(8.dp)
            ) {
                LazyColumn {
                    itemsIndexed(coordinates) { index, (x, y) ->
                        Text("[$index] x=$x, y=$y", Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}
