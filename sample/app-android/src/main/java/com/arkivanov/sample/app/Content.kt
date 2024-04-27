package com.arkivanov.sample.app

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.PredictiveBackAnimationConfig
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.sample.app.RootComponent.Child

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.GalleryContent(
    component: GalleryComponent,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = component.images) { image ->
            AsyncImage(
                model = image.url,
                contentDescription = null,
                modifier = Modifier
                    .sharedElement(
                        state = rememberSharedContentState(key = image.id),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    .fillMaxWidth()
                    .aspectRatio(1F)
                    .clickable { component.onImageClicked(image) },
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ImageContent(
    component: ImageComponent,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    AsyncImage(
        model = component.image.url,
        contentDescription = null,
        modifier = Modifier
            .sharedElement(
                state = rememberSharedContentState(key = component.image.id),
                animatedVisibilityScope = animatedVisibilityScope,
            )
            .fillMaxSize().background(Color.Black),
        contentScale = ContentScale.Fit,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalTransitionApi::class, ExperimentalAnimationApi::class)
@Composable
fun RootContent(
    component: RootComponent,
) {
    val stack by component.stack.subscribeAsState()

    SharedTransitionLayout {
        Children(
            stack = stack,
            modifier = Modifier.fillMaxSize().background(Color.Black),
            animation = stackAnimation(
                animator = fade(),
                predictiveBackAnimationConfig = PredictiveBackAnimationConfig(
                    backHandler = component.backHandler,
                    onBack = component::onBack,
                ),
            ),
        ) {
            when (val child = it.instance) {
                is Child.Gallery ->
                    GalleryContent(
                        component = child.component,
                        animatedVisibilityScope = this,
                    )

                is Child.Image ->
                    ImageContent(
                        component = child.component,
                        animatedVisibilityScope = this,
                    )
            }
        }
    }
}
