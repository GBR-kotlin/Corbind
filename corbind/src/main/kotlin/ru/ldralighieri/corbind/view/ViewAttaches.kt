@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.View
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


fun View.attaches(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(scope, true, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

suspend fun View.attaches(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(this, true, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.attaches(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {

    val listener = listener(scope, true, ::safeOffer)
    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun View.detaches(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(scope, true, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

suspend fun View.detaches(
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    val listener = listener(this, true, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.detaches(
        scope: CoroutineScope
): ReceiveChannel<Unit> = corbindReceiveChannel {

    val listener = listener(scope, true, ::safeOffer)
    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        callOnAttach: Boolean,
        emitter: (Unit) -> Boolean
) = object: View.OnAttachStateChangeListener {

    override fun onViewDetachedFromWindow(v: View) {
        if (callOnAttach && scope.isActive) { emitter(Unit) }
    }

    override fun onViewAttachedToWindow(v: View) {
        if (!callOnAttach && scope.isActive) { emitter(Unit) }
    }
}