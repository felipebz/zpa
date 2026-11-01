//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import com.felipebz.zpa.metadata.Block
import com.felipebz.zpa.metadata.FormsMetadata
import org.junit.jupiter.api.Test

class InvalidReferenceToObjectCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        val metadata = FormsMetadata()
        metadata.alerts = listOf("foo")
        metadata.blocks = listOf(Block("foo", arrayOf("item1")))
        metadata.lovs = listOf("foo")
        PlSqlCheckVerifier.verify(getPath("invalid_reference_to_object.sql"), InvalidReferenceToObjectCheck(), metadata)
    }

}
