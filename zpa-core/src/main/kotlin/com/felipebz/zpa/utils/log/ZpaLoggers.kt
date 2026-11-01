//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.utils.log

class ZpaLoggers : Loggers() {
    override fun getLogger(name: String): Logger {
        return ZpaLogger(java.util.logging.Logger.getLogger(name))
    }
}
