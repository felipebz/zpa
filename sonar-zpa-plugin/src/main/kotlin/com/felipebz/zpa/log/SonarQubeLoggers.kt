//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.log

import com.felipebz.zpa.utils.log.Logger
import com.felipebz.zpa.utils.log.Loggers
import org.slf4j.LoggerFactory

class SonarQubeLoggers : Loggers() {

    override fun getLogger(name: String): Logger {
        return SonarQubeLogger(LoggerFactory.getLogger(name))
    }

}
