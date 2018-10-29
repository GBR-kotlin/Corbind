@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.View
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

data class ViewLayoutChangeEvent(
        val view: View,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val oldLeft: Int,
        val oldTop: Int,
        val oldRight: Int,
        val oldBottom: Int
)

// -----------------------------------------------------------------------------------------------


fun View.layoutChangeEvents(
        scope: CoroutineScope,
        action: suspend (ViewLayoutChangeEvent) -> Unit
) {

    val events = scope.actor<ViewLayoutChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events::offer)
    addOnLayoutChangeListener(listener)
    events.invokeOnClose { removeOnLayoutChangeListener(listener) }
}

suspend fun View.layoutChangeEvents(
        action: suspend (ViewLayoutChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewLayoutChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(this, events::offer)
    addOnLayoutChangeListener(listener)
    events.invokeOnClose { removeOnLayoutChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.layoutChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<ViewLayoutChangeEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(this, ::offer)
    addOnLayoutChangeListener(listener)
    invokeOnClose { removeOnLayoutChangeListener(listener) }
}

@CheckResult
suspend fun View.layoutChangeEvents(): ReceiveChannel<ViewLayoutChangeEvent> = coroutineScope {

    produce<ViewLayoutChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(this, ::offer)
        addOnLayoutChangeListener(listener)
        invokeOnClose { removeOnLayoutChangeListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (ViewLayoutChangeEvent) -> Boolean
) = View.OnLayoutChangeListener {
    v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->

    if (scope.isActive) {
        emitter(ViewLayoutChangeEvent(v, left, top, right, bottom, oldLeft, oldTop, oldRight,
                oldBottom)
        )
    }
}
