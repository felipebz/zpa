//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.annotations

@Retention
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class RuleInfo(val scope: Scope = Scope.MAIN) {

    enum class Scope {
        ALL, MAIN, TEST
    }

}
