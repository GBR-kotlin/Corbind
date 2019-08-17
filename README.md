﻿[![Maven Central](https://img.shields.io/maven-central/v/ru.ldralighieri.corbind/corbind.svg)](https://search.maven.org/search?q=g:ru.ldralighieri.corbind)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a1c9a1b1d1ce4ca7a201ab93492bf6e0)](https://www.codacy.com/app/LDRAlighieri/Corbind?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=LDRAlighieri/Corbind&amp;utm_campaign=Badge_Grade)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

# Corbind

Kotlin Coroutine binding APIs for Android UI widgets from the platform and support libraries. **Supports Flow, ReceiveChannel and Actor**.


## Description

This library is for Android applications only. Help you to transform Android UI events into cold [Flow][flow], hot [ReceiveChannel][channel] or just perform an action through an [Actor][actor].


## Download

Platform bindings:
```groovy
implementation 'ru.ldralighieri.corbind:corbind:1.0.0-RC'
```

AndroidX library bindings:
```groovy
implementation 'ru.ldralighieri.corbind:corbind-core:1.0.0-RC'
implementation 'ru.ldralighieri.corbind:corbind-appcompat:1.0.0-RC'
implementation 'ru.ldralighieri.corbind:corbind-drawerlayout:1.0.0-RC'
implementation 'ru.ldralighieri.corbind:corbind-leanback:1.0.0-RC'
implementation 'ru.ldralighieri.corbind:corbind-recyclerview:1.0.0-RC'
implementation 'ru.ldralighieri.corbind:corbind-slidingpanelayout:1.0.0-RC'
implementation 'ru.ldralighieri.corbind:corbind-swiperefreshlayout:1.0.0-RC'
implementation 'ru.ldralighieri.corbind:corbind-viewpager:1.0.0-RC'
```

Google 'material' library bindings:
```groovy
implementation 'ru.ldralighieri.corbind:corbind-material:1.0.0-RC'
```


## How to use it?

If you need to get a text change events of EditText widget, simple use case with cold [Flow][flow] will look something like this:
```kotlin
findViewById<EditText>(R.id.et_name)
    .textChanges() // Flow<CharSequence>
    .onEach { /* handle text change events */ }
    .launchIn(scope)
```

If you prefer hot [ReceiveChannel][channel] and you need to get a ViewPager page selection events, then the use case will transform in something like this:
```kotlin
launch {
    findViewById<ViewPager>(R.id.vp_slides)
        .pageSelections(scope) // ReceiveChannel<Int>
        .consumeEach {
            /* handle ViewPager events */
        }
}
```

And if you just need to perform an action on button click, the easiest way will be:
```kotlin
launch {
    findViewById<AppCompatButton>(R.id.bt_confirm)
        .clicks {
            /* perform an action on View click events */
        }
}
```

Just one more traditional example of login button enabling/disabling by email and password field validation:
```kotlin
combine(
    et_email.textChanges()
        .map { Patterns.EMAIL_ADDRESS.matcher(it).matches() },

    et_password.textChanges()
        .map { it.length > 7 },
            
    transform = { email, password -> email && password }
) 
    .onEach { bt_login.isEnabled = it }
    .launchIn(scope)
```


## Missed or forgot something?

If I forgot something or you have any ideas what can be added or corrected, please create an issue or contact me directly.


## Special thanks to

[Jake Wharton][jw]. This project is inspired by [RxBinding][rx].


## License

```
Copyright 2019 Vladimir Raupov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```


[jw]: https://github.com/JakeWharton
[rx]: https://github.com/JakeWharton/RxBinding
[flow]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/index.html
[channel]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-receive-channel/index.html
[actor]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/actor.html
