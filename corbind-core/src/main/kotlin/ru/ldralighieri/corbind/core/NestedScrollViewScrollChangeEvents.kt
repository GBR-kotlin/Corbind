@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.core

import androidx.annotation.CheckResult
import androidx.core.widget.NestedScrollView
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
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer
import ru.ldralighieri.corbind.view.ViewScrollChangeEvent

// -----------------------------------------------------------------------------------------------


fun NestedScrollView.scrollChangeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewScrollChangeEvent) -> Unit
) {

    val events = scope.actor<ViewScrollChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnScrollChangeListener(listener(scope, events::offer))
    events.invokeOnClose {
        setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    }
}

suspend fun NestedScrollView.scrollChangeEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewScrollChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewScrollChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnScrollChangeListener(listener(this, events::offer))
    events.invokeOnClose {
        setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun NestedScrollView.scrollChangeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewScrollChangeEvent> = corbindReceiveChannel(capacity) {
    setOnScrollChangeListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun NestedScrollView.scrollChangeEvents(): Flow<ViewScrollChangeEvent> = channelFlow {
    setOnScrollChangeListener(listener(this, ::offer))
    awaitClose { setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (ViewScrollChangeEvent) -> Boolean
) = NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
    if (scope.isActive) {
        emitter(ViewScrollChangeEvent(v, scrollX, scrollY, oldScrollX, oldScrollY))
    }
}
