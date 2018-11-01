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


fun View.clicks(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnClickListener(listener(scope, events::offer))
    events.invokeOnClose { setOnClickListener(null) }
}

suspend fun View.clicks(
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnClickListener(listener(this, events::offer))
    events.invokeOnClose { setOnClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.clicks(
        scope: CoroutineScope
): ReceiveChannel<Unit> = corbindReceiveChannel {

    setOnClickListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnClickListener(null) }
}

@CheckResult
suspend fun View.clicks(): ReceiveChannel<Unit> = coroutineScope {

    corbindReceiveChannel<Unit> {
        setOnClickListener(listener(this@coroutineScope, ::safeOffer))
        invokeOnClose { setOnClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = View.OnClickListener {

    if (scope.isActive) { emitter(Unit) }
}
