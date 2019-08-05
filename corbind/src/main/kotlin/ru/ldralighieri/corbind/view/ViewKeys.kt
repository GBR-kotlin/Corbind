@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.KeyEvent
import android.view.View
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on key events for [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnKeyListener]
 * @param action An action to perform
 */
fun View.keys(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (KeyEvent) -> Boolean = AlwaysTrue,
        action: suspend (KeyEvent) -> Unit
) {

    val events = scope.actor<KeyEvent>(Dispatchers.Main, capacity) {
        for (key in channel) action(key)
    }

    setOnKeyListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnKeyListener(null) }
}

/**
 * Perform an action on key events for [View] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnKeyListener]
 * @param action An action to perform
 */
suspend fun View.keys(
        capacity: Int = Channel.RENDEZVOUS,
        handled: (KeyEvent) -> Boolean = AlwaysTrue,
        action: suspend (KeyEvent) -> Unit
) = coroutineScope {

    val events = actor<KeyEvent>(Dispatchers.Main, capacity) {
        for (key in channel) action(key)
    }

    setOnKeyListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnKeyListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of key events for [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnKeyListener]
 */
@CheckResult
fun View.keys(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (KeyEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<KeyEvent> = corbindReceiveChannel(capacity) {
    setOnKeyListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnKeyListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of key events for [View].
 *
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnKeyListener]
 */
@CheckResult
fun View.keys(
    handled: (KeyEvent) -> Boolean = AlwaysTrue
): Flow<KeyEvent> = channelFlow {
    setOnKeyListener(listener(this, handled, ::offer))
    awaitClose { setOnKeyListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (KeyEvent) -> Boolean,
        emitter: (KeyEvent) -> Boolean
) = View.OnKeyListener { _, _, keyEvent ->

    if (scope.isActive) {
        if (handled(keyEvent)) {
            emitter(keyEvent)
            return@OnKeyListener true
        }
    }
    return@OnKeyListener false

}
