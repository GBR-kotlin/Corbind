/*
 * Copyright 2019 Vladimir Raupov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ldralighieri.corbind.slidingpanelayout

import android.view.View
import androidx.annotation.CheckResult
import androidx.slidingpanelayout.widget.SlidingPaneLayout
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
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.safeOffer

/**
 * Perform an action on the slide offset of the pane of [SlidingPaneLayout].
 *
 * *Warning:* The actor channel uses [SlidingPaneLayout.setPanelSlideListener]. Only one actor can
 * be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SlidingPaneLayout.panelSlides(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit
) {
    val events = scope.actor<Float>(Dispatchers.Main.immediate, capacity) {
        for (slide in channel) action(slide)
    }

    setPanelSlideListener(listener(scope, events::offer))
    events.invokeOnClose { setPanelSlideListener(null) }
}

/**
 * Perform an action on the slide offset of the pane of [SlidingPaneLayout], inside new
 * [CoroutineScope].
 *
 * *Warning:* The actor channel uses [SlidingPaneLayout.setPanelSlideListener]. Only one actor can
 * be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SlidingPaneLayout.panelSlides(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit
) = coroutineScope {
    panelSlides(this, capacity, action)
}

/**
 * Create a channel of the slide offset of the pane of [SlidingPaneLayout].
 *
 * *Warning:* The created channel uses [SlidingPaneLayout.setPanelSlideListener]. Only one channel
 * can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      slidingPaneLayout.panelSlides(scope)
 *          .consumeEach { /* handle slide offset */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SlidingPaneLayout.panelSlides(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Float> = corbindReceiveChannel(capacity) {
    setPanelSlideListener(listener(scope, ::safeOffer))
    invokeOnClose { setPanelSlideListener(null) }
}

/**
 * Create a flow of the slide offset of the pane of [SlidingPaneLayout].
 *
 * *Warning:* The created flow uses [SlidingPaneLayout.setPanelSlideListener]. Only one flow can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * slidingPaneLayout.panelSlides()
 *      .onEach { /* handle slide offset */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun SlidingPaneLayout.panelSlides(): Flow<Float> = channelFlow {
    setPanelSlideListener(listener(this, ::offer))
    awaitClose { setPanelSlideListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Float) -> Boolean
) = object : SlidingPaneLayout.PanelSlideListener {

    override fun onPanelSlide(panel: View, slideOffset: Float) {
        if (scope.isActive) { emitter(slideOffset) }
    }

    override fun onPanelOpened(panel: View) = Unit
    override fun onPanelClosed(panel: View) = Unit
}
