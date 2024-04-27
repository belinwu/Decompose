package com.arkivanov.decompose.extensions.compose.stack.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.essenty.backhandler.BackCallback
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalTransitionApi
internal actual class DefaultStackAnimation<C : Any, T : Any> actual constructor(
    private val disableInputDuringAnimation: Boolean,
    private val predictiveBackAnimationConfig: PredictiveBackAnimationConfig?,
    private val selector: @Composable (
        child: Child.Created<C, T>,
        otherChild: Child.Created<C, T>,
        direction: Direction,
    ) -> StackAnimator?,
) : StackAnimation<C, T> {

    @Composable
    override operator fun invoke(
        stack: ChildStack<C, T>,
        modifier: Modifier,
        content: @Composable AnimatedVisibilityScope.(child: Child.Created<C, T>) -> Unit,
    ) {
        var currentStack by remember { mutableStateOf(stack) }
        var items by remember { mutableStateOf(getAnimationItems(newStack = currentStack, oldStack = null)) }

        LaunchedEffect(items.map { it.key }) {
            println("MyTest: Items: ${items.values}")
        }

        if (stack.active.configuration != currentStack.active.configuration) {
            val oldStack = currentStack
            currentStack = stack

            if (items.size == 1 && items.keys.single() != currentStack.active.configuration) {
                println("MyTest: set items")
                items = getAnimationItems(newStack = currentStack, oldStack = oldStack)
            }
        }

        Box(modifier = modifier) {
            items.forEach { (configuration, item) ->
                key(configuration) {
                    val transition = rememberTransition(item.transitionState)

                    if (item.transitionState.isIdle()) {
                        LaunchedEffect(Unit) {
                            println("MyTest: finished: ${item.child.configuration}")
                            if (item.direction.isExit) {
                                items -= configuration
                            } else {
                                items += (configuration to item.copy(otherChild = null))
                            }
                        }
                    }

                    WithAnimatedVisibilityScope(transition) {
                        Child(item = item, content = content)
                    }
                }
            }

            // A workaround until https://issuetracker.google.com/issues/214231672.
            // Normally only the exiting child should be disabled.
            if (disableInputDuringAnimation && (items.size > 1)) {
                Overlay(modifier = Modifier.matchParentSize())
            }
        }

        if ((predictiveBackAnimationConfig != null) && currentStack.backStack.isNotEmpty()) {
            val stackKey = remember(currentStack) { currentStack.items.map(Child<C, *>::configuration) }

            val scope = key(stackKey) { rememberCoroutineScope() }

            val callback =
                remember(stackKey) {
                    val oldStack = currentStack
                    val oldChild = stack.active
                    var oldTransitionState: SeekableTransitionState<EnterExitState>? =
                        null //= SeekableTransitionState(initialState = EnterExitState.Visible)
                    val newChild = stack.backStack.last()
                    var newTransitionState: SeekableTransitionState<EnterExitState>? =
                        null //= SeekableTransitionState(initialState = EnterExitState.PreEnter)

//                    scope.launch {
//                        joinAll(
//                            launch { oldTransitionState.seekTo(fraction = 0F, targetState = EnterExitState.PostExit) },
//                            launch { newTransitionState.seekTo(fraction = 0F, targetState = EnterExitState.Visible) },
//                        )
//                    }

                    BackCallback(
                        isEnabled = oldStack.backStack.isNotEmpty(),
                        onBackStarted = { event ->
                            oldTransitionState = SeekableTransitionState(initialState = EnterExitState.Visible)
                            newTransitionState = SeekableTransitionState(initialState = EnterExitState.PreEnter)

                            items =
                                mapOf(
                                    newChild.configuration to AnimationItem(
                                        child = newChild,
                                        direction = Direction.ENTER_BACK,
                                        transitionState = requireNotNull(newTransitionState),
                                        otherChild = oldChild,
                                    ),
                                    oldChild.configuration to AnimationItem(
                                        child = oldChild,
                                        direction = Direction.EXIT_FRONT,
                                        transitionState = requireNotNull(oldTransitionState),
                                        otherChild = newChild,
                                    ),
                                )

                            scope.launch {
                                joinAll(
                                    launch { oldTransitionState?.seekTo(fraction = event.progress, targetState = EnterExitState.PostExit) },
                                    launch { newTransitionState?.seekTo(fraction = event.progress, targetState = EnterExitState.Visible) },
                                )
                            }
                        },
                        onBackProgressed = { event ->
                            println("MyTest: progress: ${event.progress}")
                            scope.launch {
                                joinAll(
                                    launch { oldTransitionState?.seekTo(fraction = event.progress, targetState = EnterExitState.PostExit) },
                                    launch { newTransitionState?.seekTo(fraction = event.progress, targetState = EnterExitState.Visible) },
                                )
                            }
                        },
                        onBackCancelled = {
                            scope.launch {
//                                joinAll(
//                                    launch { oldTransitionState?.snapTo(EnterExitState.Visible) },
//                                    launch { newTransitionState?.snapTo(EnterExitState.PreEnter) },
//                                )

                                items = getAnimationItems(newStack = oldStack, oldStack = null)
                            }
                        },
                        onBack = {
                            scope.launch {
                                joinAll(
                                    launch { oldTransitionState?.animateTo(EnterExitState.PostExit) },
                                    launch { newTransitionState?.animateTo(EnterExitState.Visible) },
                                )

                                items = getAnimationItems(newStack = oldStack.dropLast(), oldStack = null)
                                predictiveBackAnimationConfig.onBack()
                            }
                        },
                    )
                }

            DisposableEffect(predictiveBackAnimationConfig.backHandler, callback) {
                predictiveBackAnimationConfig.backHandler.register(callback)
                onDispose { predictiveBackAnimationConfig.backHandler.unregister(callback) }
            }
        }
    }

    @Composable
    private fun AnimatedVisibilityScope.Child(
        item: AnimationItem<C, T>,
        content: @Composable AnimatedVisibilityScope.(child: Child.Created<C, T>) -> Unit,
    ) {
        val animator =
            if (item.otherChild != null) {
                selector(item.child, item.otherChild, item.direction)
            } else {
                null
            }

        val modifier = if (animator != null) with(animator) { animate(item.direction, item.isInitial) } else Modifier

        Box(modifier = modifier) {
            content(item.child)
        }
    }

    @Composable
    private fun Overlay(modifier: Modifier) {
        Box(
            modifier = modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        )
    }

    private fun getAnimationItems(newStack: ChildStack<C, T>, oldStack: ChildStack<C, T>?): Map<C, AnimationItem<C, T>> =
        when {
            oldStack == null ->
                listOf(
                    AnimationItem(
                        child = newStack.active,
                        direction = Direction.ENTER_FRONT,
                        transitionState = MutableTransitionState(EnterExitState.Visible),
                        isInitial = true,
                    )
                )

            (newStack.size < oldStack.size) && (newStack.active.configuration in oldStack.backStack) ->
                listOf(
                    AnimationItem(
                        child = newStack.active,
                        direction = Direction.ENTER_BACK,
                        transitionState = MutableTransitionState(EnterExitState.PreEnter).apply { targetState = EnterExitState.Visible },
                        otherChild = oldStack.active,
                    ),
                    AnimationItem(
                        child = oldStack.active,
                        direction = Direction.EXIT_FRONT,
                        transitionState = MutableTransitionState(EnterExitState.Visible).apply { targetState = EnterExitState.PostExit },
                        otherChild = newStack.active,
                    ),
                )

            else ->
                listOf(
                    AnimationItem(
                        child = oldStack.active,
                        direction = Direction.EXIT_BACK,
                        transitionState = MutableTransitionState(EnterExitState.Visible).apply { targetState = EnterExitState.PostExit },
                        otherChild = newStack.active,
                    ),
                    AnimationItem(
                        child = newStack.active,
                        direction = Direction.ENTER_FRONT,
                        transitionState = MutableTransitionState(EnterExitState.PreEnter).apply { targetState = EnterExitState.Visible },
                        otherChild = oldStack.active,
                    ),
                )
        }.associateBy { it.child.configuration }

    private val ChildStack<*, *>.size: Int
        get() = items.size

    private operator fun <C : Any> Iterable<Child<C, *>>.contains(config: C): Boolean =
        any { it.configuration == config }

    private data class AnimationItem<out C : Any, out T : Any>(
        val child: Child.Created<C, T>,
        val direction: Direction,
        val transitionState: TransitionState<EnterExitState>,
        val isInitial: Boolean = false,
        val otherChild: Child.Created<C, T>? = null,
    )
}

@ExperimentalAnimationApi
@Composable
internal inline fun WithAnimatedVisibilityScope(
    transition: Transition<EnterExitState>,
    block: AnimatedVisibilityScope.() -> Unit,
) {
    val scope = remember(transition) { AnimatedVisibilityScopeImpl(transition) }
    scope.block()
}

@ExperimentalAnimationApi
internal class AnimatedVisibilityScopeImpl(
    override val transition: Transition<EnterExitState>,
) : AnimatedVisibilityScope

private fun TransitionState<*>.isIdle(): Boolean =
    when (this) {
        is MutableTransitionState -> isIdle
        is SeekableTransitionState -> false
        else -> error("Unsupported transition state type: $this")
    }

private fun <C : Any, T : Any> ChildStack<C, T>.dropLast(): ChildStack<C, T> =
    ChildStack(active = backStack.last(), backStack = backStack.dropLast(1))
