package com.arkivanov.decompose.extensions.compose.stack.animation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.Child

@ExperimentalAnimationApi
@ExperimentalTransitionApi
internal expect class DefaultStackAnimation<C : Any, T : Any>(
    disableInputDuringAnimation: Boolean,
    predictiveBackAnimationConfig: PredictiveBackAnimationConfig?,
    selector: @Composable (
        child: Child.Created<C, T>,
        otherChild: Child.Created<C, T>,
        direction: Direction,
    ) -> StackAnimator?,
) : StackAnimation<C, T>
