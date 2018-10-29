@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

// -----------------------------------------------------------------------------------------------


fun Snackbar.dismisses(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val callback = callback(scope, events::offer)
    addCallback(callback)
    events.invokeOnClose { removeCallback(callback) }
}

suspend fun Snackbar.dismisses(
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    val callback = callback(this, events::offer)
    addCallback(callback)
    events.invokeOnClose { removeCallback(callback) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun Snackbar.dismisses(
        scope: CoroutineScope
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    val callback = callback(this, ::offer)
    addCallback(callback)
    invokeOnClose { removeCallback(callback) }
}

@CheckResult
suspend fun Snackbar.dismisses(): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        val callback = callback(this, ::offer)
        addCallback(callback)
        invokeOnClose { removeCallback(callback) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun callback(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = object : Snackbar.Callback() {

    override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
        if (scope.isActive) { emitter(event) }
    }
}