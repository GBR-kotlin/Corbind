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
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


fun View.longClicks(
        scope: CoroutineScope,
        handled: () -> Boolean = AlwaysTrue,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnLongClickListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnLongClickListener(null) }
}

suspend fun View.longClicks(
        handled: () -> Boolean = AlwaysTrue,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnLongClickListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnLongClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.longClicks(
        scope: CoroutineScope,
        handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<Unit> = corbindReceiveChannel {

    setOnLongClickListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnLongClickListener(null) }
}

@CheckResult
suspend fun View.longClicks(
        handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<Unit> = coroutineScope {

    corbindReceiveChannel<Unit> {
        setOnLongClickListener(listener(this@coroutineScope, handled, ::safeOffer))
        invokeOnClose { setOnLongClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: () -> Boolean,
        emitter: (Unit) -> Boolean
) = View.OnLongClickListener {

    if (scope.isActive) {
        if (handled()) {
            emitter(Unit)
            return@OnLongClickListener true
        }
    }
    return@OnLongClickListener false
}