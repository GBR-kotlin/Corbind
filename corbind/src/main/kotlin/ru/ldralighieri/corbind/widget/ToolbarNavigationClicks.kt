@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.os.Build
import android.view.View
import android.widget.Toolbar
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
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


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Toolbar.navigationClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setNavigationOnClickListener(listener(scope, events::offer))
    events.invokeOnClose { setNavigationOnClickListener(null) }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun Toolbar.navigationClicks(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setNavigationOnClickListener(listener(this, events::offer))
    events.invokeOnClose { setNavigationOnClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@CheckResult
fun Toolbar.navigationClicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setNavigationOnClickListener(listener(scope, ::safeOffer))
    invokeOnClose { setNavigationOnClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@CheckResult
fun Toolbar.navigationClicks(): Flow<Unit> = channelFlow {
    setNavigationOnClickListener(listener(this, ::offer))
    awaitClose { setNavigationOnClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = View.OnClickListener {
    if (scope.isActive) { emitter(Unit) }
}
