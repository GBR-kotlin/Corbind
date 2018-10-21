package ru.ldralighieri.corbind.view

import android.view.View
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

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

    val listener = listener(events::offer)
    addOnLayoutChangeListener(listener)
    events.invokeOnClose { removeOnLayoutChangeListener(listener) }
}

suspend fun View.layoutChangeEvents(
        action: suspend (ViewLayoutChangeEvent) -> Unit
) = coroutineScope {
    val events = actor<ViewLayoutChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val listener = listener(events::offer)
    addOnLayoutChangeListener(listener)
    events.invokeOnClose { removeOnLayoutChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun View.layoutChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<ViewLayoutChangeEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val listener = listener(::offer)
    addOnLayoutChangeListener(listener)
    invokeOnClose { removeOnLayoutChangeListener(listener) }
}

suspend fun View.layoutChangeEvents(): ReceiveChannel<ViewLayoutChangeEvent> = coroutineScope {

    produce<ViewLayoutChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        val listener = listener(::offer)
        addOnLayoutChangeListener(listener)
        invokeOnClose { removeOnLayoutChangeListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (ViewLayoutChangeEvent) -> Boolean
) = View.OnLayoutChangeListener {
    v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
    emitter(ViewLayoutChangeEvent(v, left, top, right, bottom, oldLeft, oldTop, oldRight,
            oldBottom)
    )
}
