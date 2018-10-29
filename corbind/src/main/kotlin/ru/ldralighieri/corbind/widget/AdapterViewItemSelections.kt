@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

// -----------------------------------------------------------------------------------------------


fun <T : Adapter> AdapterView<T>.itemSelections(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (position in channel) action(position)
    }

    events.offer(selectedItemPosition)
    onItemSelectedListener = listener(scope, events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.itemSelections(
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (position in channel) action(position)
    }

    events.offer(selectedItemPosition)
    onItemSelectedListener = listener(this, events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun <T : Adapter> AdapterView<T>.itemSelections(
        scope: CoroutineScope
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(selectedItemPosition)
    onItemSelectedListener = listener(this, ::offer)
    invokeOnClose { onItemSelectedListener = null }
}

@CheckResult
suspend fun <T : Adapter> AdapterView<T>.itemSelections(): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        offer(selectedItemPosition)
        onItemSelectedListener = listener(this, ::offer)
        invokeOnClose { onItemSelectedListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = object : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        onEvent(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>) { onEvent(AdapterView.INVALID_POSITION) }

    private fun onEvent(position: Int) {
        if (scope.isActive) { emitter(position) }
    }
}