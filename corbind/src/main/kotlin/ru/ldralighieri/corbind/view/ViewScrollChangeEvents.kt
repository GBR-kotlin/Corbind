package ru.ldralighieri.corbind.view

import android.os.Build
import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive

// -----------------------------------------------------------------------------------------------

data class ViewScrollChangeEvent(
        val view: View,
        val scrollX: Int,
        val scrollY: Int,
        val oldScrollX: Int,
        val oldScrollY: Int
)

// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.M)
fun View.scrollChangeEvents(
        scope: CoroutineScope,
        action: suspend (ViewScrollChangeEvent) -> Unit
) {

    val events = scope.actor<ViewScrollChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnScrollChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnScrollChangeListener(null) }
}

@RequiresApi(Build.VERSION_CODES.M)
suspend fun View.scrollChangeEvents(
        action: suspend (ViewScrollChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewScrollChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnScrollChangeListener(listener(this, events::offer))
    events.invokeOnClose { setOnScrollChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.M)
@CheckResult
fun View.scrollChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<ViewScrollChangeEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnScrollChangeListener(listener(this, ::offer))
    invokeOnClose { setOnScrollChangeListener(null) }
}

@RequiresApi(Build.VERSION_CODES.M)
@CheckResult
suspend fun View.scrollChangeEvents(): ReceiveChannel<ViewScrollChangeEvent> = coroutineScope {

    produce<ViewScrollChangeEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnScrollChangeListener(listener(this, ::offer))
        invokeOnClose { setOnScrollChangeListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (ViewScrollChangeEvent) -> Boolean
) = View.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->

    if (scope.isActive) {
        emitter(ViewScrollChangeEvent(v, scrollX, scrollY, oldScrollX, oldScrollY))
    }
}