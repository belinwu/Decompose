package com.arkivanov.decompose.extensions.compose.stack.animation

import com.arkivanov.essenty.backhandler.BackHandler

class PredictiveBackAnimationConfig(
    val backHandler: BackHandler,
    val onBack: () -> Unit,
)
