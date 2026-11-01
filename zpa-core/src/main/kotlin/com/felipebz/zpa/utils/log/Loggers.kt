//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.utils.log

abstract class Loggers {

    protected abstract fun getLogger(name: String): Logger

    companion object {
        var factory: Loggers = ZpaLoggers()

        fun getLogger(name: Class<*>): Logger {
            return factory.getLogger(name.name)
        }
    }

}
