@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.MenuItem
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
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------

/**
 * An action view event on a menu item.
 */
sealed class MenuItemActionViewEvent {
    abstract val menuItem: MenuItem
}

data class MenuItemActionViewCollapseEvent(
        override val menuItem: MenuItem
) : MenuItemActionViewEvent()

data class MenuItemActionViewExpandEvent(
        override val menuItem: MenuItem
) : MenuItemActionViewEvent()

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on action view events for `menuItem`.
 */
fun MenuItem.actionViewEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
        action: suspend (MenuItemActionViewEvent) -> Boolean
) {

    val events = scope.actor<MenuItemActionViewEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnActionExpandListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnActionExpandListener(null) }
}

/**
 * Perform an action on action view events for `menuItem` inside new CoroutineScope.
 */
suspend fun MenuItem.actionViewEvents(
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
        action: suspend (MenuItemActionViewEvent) -> Unit
) = coroutineScope {

    val events = actor<MenuItemActionViewEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnActionExpandListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnActionExpandListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of action view events for `menuItem`.
 */
@CheckResult
fun MenuItem.actionViewEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MenuItemActionViewEvent> = corbindReceiveChannel(capacity) {
    setOnActionExpandListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnActionExpandListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of action view events for `menuItem`.
 */
@CheckResult
fun MenuItem.actionViewEvents(
    handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue
): Flow<MenuItemActionViewEvent> = channelFlow {
    setOnActionExpandListener(listener(this, handled, ::offer))
    awaitClose { setOnActionExpandListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * listener of action view events for `menuItem`.
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (MenuItemActionViewEvent) -> Boolean,
        emitter: (MenuItemActionViewEvent) -> Boolean
) = object : MenuItem.OnActionExpandListener {

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        return onEvent(MenuItemActionViewExpandEvent(item))
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        return onEvent(MenuItemActionViewCollapseEvent(item))
    }

    private fun onEvent(event: MenuItemActionViewEvent): Boolean {
        if (scope.isActive) {
            if (handled(event)) {
                emitter(event)
                return true
            }
        }
        return false
    }
}
