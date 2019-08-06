@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.SeekBar
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
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------

sealed class SeekBarChangeEvent {
    abstract val view: SeekBar
}

data class SeekBarProgressChangeEvent(
        override val view: SeekBar,
        val progress: Int,
        val fromUser: Boolean
) : SeekBarChangeEvent()

data class SeekBarStartChangeEvent(
        override val view: SeekBar
) : SeekBarChangeEvent()

data class SeekBarStopChangeEvent(
        override val view: SeekBar
) : SeekBarChangeEvent()

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on progress change events for [SeekBar].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
private fun SeekBar.changeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (SeekBarChangeEvent) -> Unit
) {

    val events = scope.actor<SeekBarChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    setOnSeekBarChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnSeekBarChangeListener(null) }
}

/**
 * Perform an action on progress change events for [SeekBar] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
private suspend fun SeekBar.changeEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (SeekBarChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<SeekBarChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@changeEvents))
    setOnSeekBarChangeListener(listener(this, events::offer))
    events.invokeOnClose { setOnSeekBarChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of progress change events for [SeekBar].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
private fun SeekBar.changeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<SeekBarChangeEvent> = corbindReceiveChannel(capacity) {
    offer(initialValue(this@changeEvents))
    setOnSeekBarChangeListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnSeekBarChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of progress change events for [SeekBar].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun SeekBar.changeEvents(): Flow<SeekBarChangeEvent> = channelFlow {
    offer(initialValue(this@changeEvents))
    setOnSeekBarChangeListener(listener(this, ::offer))
    awaitClose { setOnSeekBarChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun initialValue(seekBar: SeekBar): SeekBarChangeEvent =
        SeekBarProgressChangeEvent(seekBar, seekBar.progress, false)


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (SeekBarChangeEvent) -> Boolean
) = object : SeekBar.OnSeekBarChangeListener {

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        onEvent(SeekBarProgressChangeEvent(seekBar, progress, fromUser))
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        onEvent(SeekBarStartChangeEvent(seekBar))
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        onEvent(SeekBarStopChangeEvent(seekBar))
    }

    private fun onEvent(event: SeekBarChangeEvent)  {
        if (scope.isActive) { emitter(event) }
    }

}
