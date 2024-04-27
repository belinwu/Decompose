package com.arkivanov.decompose.extensions.compose.stack.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalAnimationApi::class)
internal class DefaultStackAnimator(
    private val animationSpec: FiniteAnimationSpec<Float> = tween(),
    private val frame: @Composable (factor: Float, direction: Direction) -> Modifier
) : StackAnimator {

    @Composable
    override fun AnimatedVisibilityScope.animate(direction: Direction, isInitial: Boolean): Modifier {
        val factor by transition.animateFloat(transitionSpec = { animationSpec }) { state ->
            when (state) {
                EnterExitState.PreEnter -> 1F
                EnterExitState.Visible -> 0F
                EnterExitState.PostExit -> -1F
            }
        }

        return frame(if (direction.isFront) factor else -factor, direction)
    }
}
