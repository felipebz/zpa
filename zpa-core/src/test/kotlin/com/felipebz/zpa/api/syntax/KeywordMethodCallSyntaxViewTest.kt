/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
