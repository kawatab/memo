/**
 * memo/App.kt - Entry point
 *
 * Copyright (c) 2019 Yasuhiro Yamakawa <kawatab@yahoo.co.jp>
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/mit-license.php
 */

package io.github.kawatab.memo

import javafx.application.Application


class App {
    val greeting: String
    get() {
	return "Hello world."
    }
}

fun main(args: Array<String>) {
    Application.launch(Memo::class.java, *args)
}
