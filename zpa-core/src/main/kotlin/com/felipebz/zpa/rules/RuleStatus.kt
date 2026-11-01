//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

enum class RuleStatus {
    BETA, DEPRECATED, READY, REMOVED;

    companion object {
        fun defaultStatus(): RuleStatus {
            return READY
        }
    }
}
