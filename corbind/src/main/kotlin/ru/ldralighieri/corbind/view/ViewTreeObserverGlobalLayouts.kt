@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

// -----------------------------------------------------------------------------------------------


fun View.globalLayouts(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(scope, events::offer)
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
}

suspend fun View.globalLayouts(
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(this, events::offer)
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.globalLayouts(
        scope: CoroutineScope
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(this, ::offer)
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    invokeOnClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
}

@CheckResult
suspend fun View.globalLayouts(): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(this, ::offer)
        viewTreeObserver.addOnGlobalLayoutListener(listener)
        invokeOnClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = ViewTreeObserver.OnGlobalLayoutListener {

    if (scope.isActive) { emitter(Unit) }
}