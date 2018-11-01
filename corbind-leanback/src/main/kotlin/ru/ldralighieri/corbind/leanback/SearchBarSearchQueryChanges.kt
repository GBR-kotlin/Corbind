@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.leanback

import androidx.annotation.CheckResult
import androidx.leanback.widget.SearchBar
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


fun SearchBar.searchQueryChanges(
        scope: CoroutineScope,
        action: suspend (String) -> Unit
) {

    val events = scope.actor<String>(Dispatchers.Main, Channel.CONFLATED) {
        for (query in channel) action(query)
    }

    setSearchBarListener(listener(scope, events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}

suspend fun SearchBar.searchQueryChanges(
        action: suspend (String) -> Unit
) = coroutineScope {

    val events = actor<String>(Dispatchers.Main, Channel.CONFLATED) {
        for (query in channel) action(query)
    }

    setSearchBarListener(listener(this, events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SearchBar.searchQueryChanges(
        scope: CoroutineScope
): ReceiveChannel<String> = corbindReceiveChannel {

    setSearchBarListener(listener(scope, ::safeOffer))
    invokeOnClose { setSearchBarListener(null) }
}

@CheckResult
suspend fun SearchBar.searchQueryChanges(): ReceiveChannel<String> = coroutineScope {

    corbindReceiveChannel<String> {
        setSearchBarListener(listener(this@coroutineScope, ::safeOffer))
        invokeOnClose { setSearchBarListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (String) -> Boolean
) = object : SearchBar.SearchBarListener {

    override fun onSearchQueryChange(query: String) {
        if (scope.isActive) { emitter(query) }
    }

    override fun onSearchQuerySubmit(query: String) {  }
    override fun onKeyboardDismiss(query: String) {  }
}