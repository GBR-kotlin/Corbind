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

package ru.ldralighieri.corbind.material

import android.view.View
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
 * Perform an action on the state change events from [View] on [BottomSheetBehavior].
 *
 * *Warning:* The created actor uses [BottomSheetBehavior.setBottomSheetCallback]. Only one actor
 * can be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.stateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (state in channel) action(state)
    }

    val behavior = getBehavior(this@stateChanges)
    events.offer(behavior.state)
    behavior.bottomSheetCallback = callback(scope, events::offer)
    events.invokeOnClose { behavior.bottomSheetCallback = null }
}

/**
 * Perform an action on the state change events from [View] on [BottomSheetBehavior], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [BottomSheetBehavior.setBottomSheetCallback]. Only one actor
 * can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.stateChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    stateChanges(this, capacity, action)
}

/**
 * Create a channel which emits the state change events from [View] on [BottomSheetBehavior].
 *
 * *Warning:* The created channel uses [BottomSheetBehavior.setBottomSheetCallback]. Only one
 * channel can be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      bottomSheetBehavior.stateChanges(scope)
 *          .consumeEach { /* handle state change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.stateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    val behavior = getBehavior(this@stateChanges)
    safeOffer(behavior.state)
    behavior.bottomSheetCallback = callback(scope, ::safeOffer)
    invokeOnClose { behavior.bottomSheetCallback = null }
}

/**
 * Create a flow which emits the state change events from [View] on [BottomSheetBehavior].
 *
 * *Warning:* The created flow uses [BottomSheetBehavior.setBottomSheetCallback]. Only one flow can
 * be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * bottomSheetBehavior.stateChanges()
 *      .onEach { /* handle state change */ }
 *      .launchIn(scope)
 *
 * // drop initial value
 * bottomSheetBehavior.stateChanges()
 *      .drop(1)
 *      .onEach { /* handle state change */ }
 *      .launchIn(scope)
 * ```
 */
fun View.stateChanges(): Flow<Int> = channelFlow {
    val behavior = getBehavior(this@stateChanges)
    offer(behavior.state)
    behavior.bottomSheetCallback = callback(this, ::offer)
    awaitClose { behavior.bottomSheetCallback = null }
}

@CheckResult
private fun getBehavior(view: View): BottomSheetBehavior<*> {
    val params = view.layoutParams as? CoordinatorLayout.LayoutParams
        ?: throw IllegalArgumentException("The view is not in a Coordinator Layout.")
    return params.behavior as BottomSheetBehavior<*>?
        ?: throw IllegalStateException("There's no behavior set on this view.")
}

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean
) = object : BottomSheetBehavior.BottomSheetCallback() {

    override fun onSlide(bottomSheet: View, slideOffset: Float) { }

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        if (scope.isActive) { emitter(newState) }
    }
}
