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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------

sealed class AdapterViewSelectionEvent {
    abstract val view: AdapterView<*>
}

data class AdapterViewItemSelectionEvent(
        override val view: AdapterView<*>,
        val selectedView: View,
        val position: Int,
        val id: Long
) : AdapterViewSelectionEvent()

data class AdapterViewNothingSelectionEvent(
        override val view: AdapterView<*>
) : AdapterViewSelectionEvent()

// -----------------------------------------------------------------------------------------------


fun <T : Adapter> AdapterView<T>.selectionEvents(
        scope: CoroutineScope,
        action: suspend (AdapterViewSelectionEvent) -> Unit
) {

    val events = scope.actor<AdapterViewSelectionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    onItemSelectedListener = listener(scope, events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.selectionEvents(
        action: suspend (AdapterViewSelectionEvent) -> Unit
) = coroutineScope {

    val events = actor<AdapterViewSelectionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@selectionEvents))
    onItemSelectedListener = listener(this, events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun <T : Adapter> AdapterView<T>.selectionEvents(
        scope: CoroutineScope
): ReceiveChannel<AdapterViewSelectionEvent> = corbindReceiveChannel {

    offer(initialValue(this@selectionEvents))
    onItemSelectedListener = listener(scope, ::safeOffer)
    invokeOnClose { onItemSelectedListener = null }
}

@CheckResult
suspend fun <T : Adapter> AdapterView<T>.selectionEvents()
        : ReceiveChannel<AdapterViewSelectionEvent> = coroutineScope {

    corbindReceiveChannel<AdapterViewSelectionEvent> {
        offer(initialValue(this@selectionEvents))
        onItemSelectedListener = listener(this@coroutineScope, ::safeOffer)
        invokeOnClose { onItemSelectedListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun <T : Adapter> initialValue(adapterView: AdapterView<T>): AdapterViewSelectionEvent {
    return if (adapterView.selectedItemPosition == AdapterView.INVALID_POSITION) {
        AdapterViewNothingSelectionEvent(adapterView)
    } else {
        AdapterViewItemSelectionEvent(adapterView, adapterView.selectedView,
                adapterView.selectedItemPosition, adapterView.selectedItemId)
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (AdapterViewSelectionEvent) -> Boolean
) = object : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        onEvent(AdapterViewItemSelectionEvent(parent, view, position, id))
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        onEvent(AdapterViewNothingSelectionEvent(parent))
    }

    private fun onEvent(event: AdapterViewSelectionEvent) {
        if (scope.isActive) { emitter(event) }
    }
}