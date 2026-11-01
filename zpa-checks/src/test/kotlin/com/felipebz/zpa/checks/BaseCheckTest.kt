//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import org.junit.jupiter.api.BeforeEach
import java.util.*

abstract class BaseCheckTest {

    @BeforeEach
    fun setUp() {
        Locale.setDefault(Locale.ENGLISH)
    }

    protected fun getPath(filename: String) = defaultResourceFolder + filename

    companion object {
        private const val defaultResourceFolder = "src/test/resources/checks/"
    }

}
