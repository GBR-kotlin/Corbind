@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.MotionEvent
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
 * Perform an action on touch events for [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]
 * @param action An action to perform
 */
fun View.touches(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) {

    val events = scope.actor<MotionEvent>(Dispatchers.Main, capacity) {
        for (motion in channel) action(motion)
    }

    setOnTouchListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnTouchListener(null) }
}

/**
 * Perform an action on touch events for [View] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]
 * @param action An action to perform
 */
suspend fun View.touches(
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MotionEvent) -> Boolean = AlwaysTrue,
        action: suspend (MotionEvent) -> Unit
) = coroutineScope {

    val events = actor<MotionEvent>(Dispatchers.Main, capacity) {
        for (motion in channel) action(motion)
    }

    setOnTouchListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnTouchListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of touch events for [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]
 */
@CheckResult
fun View.touches(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = corbindReceiveChannel(capacity) {
    setOnTouchListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnTouchListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of touch events for [View].
 *
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]
 */
@CheckResult
fun View.touches(
    handled: (MotionEvent) -> Boolean = AlwaysTrue
): Flow<MotionEvent> = channelFlow {
    setOnTouchListener(listener(this, handled, ::offer))
    awaitClose { setOnTouchListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (MotionEvent) -> Boolean,
        emitter: (MotionEvent) -> Boolean
) = View.OnTouchListener { _, motionEvent ->

    if (scope.isActive) {
        if (handled(motionEvent)) {
            emitter(motionEvent)
            return@OnTouchListener true
        }
    }
    return@OnTouchListener false

}
