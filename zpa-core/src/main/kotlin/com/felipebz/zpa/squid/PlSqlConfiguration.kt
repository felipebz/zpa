//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.squid

import java.nio.charset.Charset

class PlSqlConfiguration @JvmOverloads constructor(
    val charset: Charset,
    val isErrorRecoveryEnabled: Boolean = false,
    /** Conditional compilation flags and database version; the same value must reach every lexer of an analysis. */
    val conditionalCompilation: ConditionalCompilationSettings = ConditionalCompilationSettings.NONE
)
