---
layout: page
title: Corbind
subtitle: corbind-recyclerview module
show-avatar: false
description: Coroutines binding APIs for Android UI widgets from the platform and support libraries. Androidx recyclerview bindings.
tags: [coroutines binding,coroutine binding,coroutines,coroutine,corbind,kotlin,android,androidx,receivechannel,flow,data binding,androidx recyclerview bindings]
---

# corbind-recyclerview

To add androidx recyclerview bindings, import `corbind-recyclerview` module:

```groovy
implementation 'ru.ldralighieri.corbind:corbind-recyclerview:1.1.0'
```

## List of extensions

Component | Extension | Description
--|---|--
**RecyclerView** | `childAttachStateChangeEvents` | Called when a view is attached to or detached from the RecyclerView.
                  | `flingEvents` | Handle a fling given the velocities in both x and y directions
                  | `scrollEvents` | Called when a scrolling event has occurred on that RecyclerView.
                  | `scrollStateChanges` | Called when RecyclerView's scroll state changes.
**RecyclerView.Adapter** | `dataChanges` | Called when the RecyclerView's adapter data has been changed  |   |


## Example

```kotlin
rv.scrollStateChanges() // Flow<Int>
    .onEach { /* handle RecyclerView scroll state change events */ }
    .launchIn(scope)
```
