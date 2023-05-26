package com.pwitko.feature.blecomm.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
internal fun DisposableEffectWithLifeCycle(
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onPause: () -> Unit = {},
    onResume: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnStart = rememberUpdatedState(onStart)
    val currentOnStop = rememberUpdatedState(onStop)
    val currentOnPause = rememberUpdatedState(onPause)
    val currentOnResume = rememberUpdatedState(onResume)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { source, event ->
            if (source == lifecycleOwner) {
                when(event) {
                    Lifecycle.Event.ON_START -> currentOnStart.value.invoke()
                    Lifecycle.Event.ON_STOP -> currentOnStop.value.invoke()
                    Lifecycle.Event.ON_PAUSE -> currentOnPause.value.invoke()
                    Lifecycle.Event.ON_RESUME -> currentOnResume.value.invoke()
                    else -> Unit
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}