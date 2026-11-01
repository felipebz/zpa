//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.syntax

import com.felipebz.zpa.TestPlSqlVisitorRunner
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlVisitor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ZpaExperimentalApi::class)
class KeywordMethodCallSyntaxViewTest {

    @Test
    fun preservesKeywordLikeMemberComponents() {
        val calls = mutableListOf<MethodCall>()
        TestPlSqlVisitorRunner.scanFile(fixture(), null, RecordingVisitor(calls))

        assertThat(calls.map { it.qualifier to it.name }).containsExactly(
            listOf("collection") to "exists",
            listOf("collection") to "delete",
            listOf("collection") to "trim",
            listOf("collection") to "extend",
            listOf("collection") to "next",
        )
    }

    private fun fixture() = File("src/test/resources/syntax/keyword-method-calls.sql")

    private class RecordingVisitor(private val calls: MutableList<MethodCall>) : PlSqlVisitor() {
        override fun init() {
            subscribeTo(SyntaxViews.METHOD_CALL) { calls += it }
        }
    }
}
