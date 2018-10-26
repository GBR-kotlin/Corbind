package ru.ldralighieri.corbind.appcompat

import android.view.View
import androidx.annotation.CheckResult
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun Toolbar.navigationClicks(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setNavigationOnClickListener(listener(events::offer))
    events.invokeOnClose { setNavigationOnClickListener(null) }
}

suspend fun Toolbar.navigationClicks(
        action: suspend () -> Unit
) = coroutineScope {
    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setNavigationOnClickListener(listener(events::offer))
    events.invokeOnClose { setNavigationOnClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun Toolbar.navigationClicks(
        scope: CoroutineScope
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setNavigationOnClickListener(listener(::offer))
    invokeOnClose { setNavigationOnClickListener(null) }
}

@CheckResult
suspend fun Toolbar.navigationClicks(): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        setNavigationOnClickListener(listener(::offer))
        invokeOnClose { setNavigationOnClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        emitter: (Unit) -> Boolean
) = View.OnClickListener { emitter(Unit) }