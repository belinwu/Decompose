@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE") // Workaround for KTIJ-22326

package com.arkivanov.sample.shared.counters.counter

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.sample.shared.dialog.DialogContent

@Composable
internal fun CounterContent(component: CounterComponent, modifier: Modifier = Modifier) {
    val model by component.model.subscribeAsState()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            title = {
                Text(
                    text = model.title,
                    modifier = Modifier.testTag("title"),
                )
            },
            navigationIcon = model.isBackEnabled.takeIf { it }?.let {
                {
                    IconButton(onClick = component::onPrevClicked) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back button",
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.weight(1F))

        Text(
            text = model.text,
            modifier = Modifier.testTag("text"),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = component::onInfoClicked) {
            Text(text = "Info")
        }

        Button(
            onClick = component::onNextClicked,
            modifier = Modifier.testTag("next"),
        ) {
            Text(text = "Next")
        }

        Button(
            onClick = component::onPrevClicked,
            modifier = Modifier.testTag("prev"),
            enabled = model.isBackEnabled,
        ) {
            Text(text = "Prev")
        }

        Spacer(modifier = Modifier.weight(1F))
    }

    val dialogSlot by component.dialogSlot.subscribeAsState()
    dialogSlot.child?.instance?.also {
        DialogContent(dialogComponent = it)
    }
}

@Preview
@Composable
internal fun CounterContentPreview() {
    CounterContent(component = PreviewCounterComponent())
}
