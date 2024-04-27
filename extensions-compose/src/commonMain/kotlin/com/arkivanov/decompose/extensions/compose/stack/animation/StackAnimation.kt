package com.arkivanov.decompose.extensions.compose.stack.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack

/**
 * Tracks the [ChildStack] changes and animates between child widget.
 */
fun interface StackAnimation<C : Any, T : Any> {

    @Composable
    operator fun invoke(
        stack: ChildStack<C, T>,
        modifier: Modifier,
        content: @Composable AnimatedVisibilityScope.(child: Child.Created<C, T>) -> Unit,
    )
}

/**
 * Creates an implementation of [StackAnimation] that allows different [StackAnimator]s.
 *
 * @param disableInputDuringAnimation disables input and touch events while animating, default value is `true`.
 * @param selector provides a [StackAnimator] for the current [Child], other [Child] and [Direction].
 */
@ExperimentalTransitionApi
@ExperimentalAnimationApi
fun <C : Any, T : Any> stackAnimation(
    disableInputDuringAnimation: Boolean = true,
    predictiveBackAnimationConfig: PredictiveBackAnimationConfig? = null,
    selector: (child: Child.Created<C, T>, otherChild: Child.Created<C, T>, direction: Direction) -> StackAnimator?,
): StackAnimation<C, T> =
    DefaultStackAnimation(
        disableInputDuringAnimation = disableInputDuringAnimation,
        predictiveBackAnimationConfig = predictiveBackAnimationConfig,
        selector = { child, otherChild, direction ->
            remember(child.configuration, otherChild.configuration, direction) {
                selector(child, otherChild, direction)
            }
        },
    )

/**
 * Creates an implementation of [StackAnimation] that allows different [StackAnimator]s.
 *
 * @param disableInputDuringAnimation disables input and touch events while animating, default value is `true`.
 * @param selector provides a [StackAnimator] for the current [Child].
 */
@ExperimentalTransitionApi
@ExperimentalAnimationApi
fun <C : Any, T : Any> stackAnimation(
    disableInputDuringAnimation: Boolean = true,
    predictiveBackAnimationConfig: PredictiveBackAnimationConfig? = null,
    selector: (child: Child.Created<C, T>) -> StackAnimator?,
): StackAnimation<C, T> =
    DefaultStackAnimation(
        disableInputDuringAnimation = disableInputDuringAnimation,
        predictiveBackAnimationConfig = predictiveBackAnimationConfig,
        selector = { child, _, _ ->
            remember(child.configuration) {
                selector(child)
            }
        },
    )

/**
 * Creates an implementation of [StackAnimation] with the provided [StackAnimator].
 *
 * @param animator a [StackAnimator] to be used for animation, default is [fade].
 * @param disableInputDuringAnimation disables input and touch events while animating, default value is `true`.
 */
@ExperimentalTransitionApi
@ExperimentalAnimationApi
fun <C : Any, T : Any> stackAnimation(
    animator: StackAnimator = fade(),
    disableInputDuringAnimation: Boolean = true,
    predictiveBackAnimationConfig: PredictiveBackAnimationConfig? = null,
): StackAnimation<C, T> =
    DefaultStackAnimation(
        disableInputDuringAnimation = disableInputDuringAnimation,
        predictiveBackAnimationConfig = predictiveBackAnimationConfig,
        selector = { _, _, _ -> animator },
    )
