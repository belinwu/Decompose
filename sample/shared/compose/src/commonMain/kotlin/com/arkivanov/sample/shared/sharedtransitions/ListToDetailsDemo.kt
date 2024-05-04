package com.arkivanov.sample.shared.sharedtransitions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.sample.shared.ImageResourceId
import com.arkivanov.sample.shared.painterResource

sealed class Screen {
    object List : Screen()
    data class Details(val item: Int) : Screen()
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ListToDetailsDemo() {
    var state by remember {
        mutableStateOf<Screen>(Screen.List)
    }
    val images = ImageResourceId.entries.take(3)
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(state, label = "", contentKey = { it::class },
            transitionSpec = {
                if (initialState == Screen.List) {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                } else {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                }
            }) {
            when (it) {
                Screen.List -> {
                    LazyColumn {
                        items(50) { item ->
                            Row(modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    state = Screen.Details(item)
                                }
                                .fillMaxWidth()) {
                                Image(
                                    painter = painterResource(images[item % 3]),
                                    modifier = Modifier
                                        .size(100.dp)
                                        .then(
                                            if (item % 3 < 2) {
                                                Modifier.sharedElementWithCallerManagedVisibility(
                                                    rememberSharedContentState(
                                                        key = "item-image$item"
                                                    ),
                                                    visible = state is Screen.List,
                                                )
                                            } else Modifier
                                        ),
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null
                                )
                                Spacer(Modifier.size(15.dp))
                                Text("Item $item")
                            }
                        }
                    }
                }

                is Screen.Details -> {
                    val item = it.item
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            state = Screen.List
                        }) {
                        Image(
                            painter = painterResource(images[item % 3]),
                            modifier = Modifier
                                .then(
                                    if (item % 3 < 2) {
                                        Modifier.sharedElementWithCallerManagedVisibility(
                                            rememberSharedContentState(key = "item-image$item"),
                                            visible = state is Screen.Details,
                                        )
                                    } else Modifier
                                )
                                .fillMaxWidth(),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                        Text(
                            "Item $item",
                            fontSize = 23.sp
                        )
                    }
                }
            }
        }
    }
}
