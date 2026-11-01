//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import org.junit.jupiter.api.Test

class IneffectiveUtPlSqlAnnotationCheckTest : BaseCheckTest() {

    @Test
    fun reportsRecognizedAnnotationsInPackageBodiesOnly() {
        PlSqlCheckVerifier.verify(
            getPath("ineffective_utplsql_annotation.sql"),
            IneffectiveUtPlSqlAnnotationCheck()
        )
    }
}

