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

package ru.ldralighieri.corbind.sample

import android.os.Bundle
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.flow.*
import ru.ldralighieri.corbind.sample.core.CorbindActivity
import ru.ldralighieri.corbind.sample.core.extensions.hideSoftInput
import ru.ldralighieri.corbind.swiperefreshlayout.refreshes
import ru.ldralighieri.corbind.view.clicks
import ru.ldralighieri.corbind.widget.editorActionEvents
import ru.ldralighieri.corbind.widget.textChanges

class LoginActivity : CorbindActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        bindViews()
    }

    private fun bindViews() {

        combine(
            et_email.textChanges()
                .map { Patterns.EMAIL_ADDRESS.matcher(it).matches() },

            et_password.textChanges()
                .map { it.length > 7 },

            transform = { email, password -> email && password }
        )
            .onEach { bt_login.isEnabled = it }
            .launchIn(this)

        merge(
            bt_login.clicks(),

            et_password.editorActionEvents()
                .filter { it.actionId == EditorInfo.IME_ACTION_DONE }
                .filter { bt_login.isEnabled }
                .onEach { hideSoftInput() }
        )
            .onEach { Toast.makeText(this, R.string.login_success_message, Toast.LENGTH_SHORT).show() }
            .launchIn(this)

        swipe.refreshes()
            .onEach {
                et_email.text?.clear()
                et_password.text?.clear()
                swipe.isRefreshing = false
                hideSoftInput()
                Toast.makeText(this, R.string.login_swipe_message, Toast.LENGTH_SHORT).show()
            }
            .launchIn(this)
    }
}
