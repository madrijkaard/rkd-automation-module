package com.rkd

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.rkd.security.SQLiteSecurity
import java.awt.Dimension
import javax.swing.JFrame

fun main() = application {

    SQLiteSecurity.initializeDatabase()

    Window(onCloseRequest = {
        exitApplication()
    }, title = "Prepare Automation Module", resizable = false) {

        val currentWindow = this.window

        currentWindow.minimumSize = Dimension(800, 600)
        currentWindow.extendedState = JFrame.MAXIMIZED_BOTH
        currentWindow.isResizable = false

        App(exitApplication = {
            SQLiteSecurity.shutdownDatabase()
            exitApplication()
        })
    }
}
