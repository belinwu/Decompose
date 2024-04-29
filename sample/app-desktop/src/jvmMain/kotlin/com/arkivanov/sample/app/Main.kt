package com.arkivanov.sample.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.arkivanov.sample.shared.dynamicfeatures.dynamicfeature.DefaultFeatureInstaller
import com.arkivanov.sample.shared.readSerializableContainer
import com.arkivanov.sample.shared.root.DefaultRootComponent
import com.arkivanov.sample.shared.root.RootContent
import com.arkivanov.sample.shared.writeToFile
import com.badoo.reaktive.coroutinesinterop.asScheduler
import com.badoo.reaktive.scheduler.overrideSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File

private const val SAVED_STATE_FILE_NAME = "saved_state.dat"

@OptIn(ExperimentalDecomposeApi::class, ExperimentalCoroutinesApi::class)
fun main() {
    overrideSchedulers(main = Dispatchers.Main::asScheduler)

    val lifecycle = LifecycleRegistry()
    val stateKeeper = StateKeeperDispatcher(File(SAVED_STATE_FILE_NAME).readSerializableContainer())

    val root =
        runOnUiThread {
            DefaultRootComponent(
                componentContext = DefaultComponentContext(
                    lifecycle = lifecycle,
                    stateKeeper = stateKeeper,
                ),
                featureInstaller = DefaultFeatureInstaller,
            )
        }

    application {
        val windowState = rememberWindowState()
        var isCloseRequested by remember { mutableStateOf(false) }

        Window(
            onCloseRequest = { isCloseRequested = true },
            state = windowState,
            title = "Decompose Sample"
        ) {
            LifecycleController(
                lifecycleRegistry = lifecycle,
                windowState = windowState,
                windowInfo = LocalWindowInfo.current,
            )

            RootContent(root)

            if (isCloseRequested) {
                SaveStateDialog(
                    onSaveState = { stateKeeper.save().writeToFile(File(SAVED_STATE_FILE_NAME)) },
                    onExitApplication = ::exitApplication,
                    onDismiss = { isCloseRequested = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SaveStateDialog(
    onSaveState: () -> Unit,
    onExitApplication: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel")
                }

                TextButton(onClick = onExitApplication) {
                    Text(text = "No")
                }

                TextButton(
                    onClick = {
                        onSaveState()
                        onExitApplication()
                    }
                ) {
                    Text(text = "Yes")
                }
            }
        },
        title = { Text(text = "Decompose Sample") },
        text = { Text(text = "Do you want to save the application's state?") },
        modifier = Modifier.width(400.dp),
    )
}
