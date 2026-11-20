//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

@OptIn(ZpaExperimentalApi::class)
class ByteOrderMarkAnalysisTest {

    private val configuration = PlSqlConfiguration(StandardCharsets.UTF_8, false)

    @Test
    fun aFileWithAByteOrderMarkParsesAndGetsTheSameIssues() {
        val body = """
            create or replace package body bin_pkg is
              procedure p(p_unused number) is
              begin
                null;
              end;
            end;
        """.trimIndent()
        PlSqlParser.create(configuration).parse(BOM + body)

        val withMark = issues(mapOf("bin_pkg.pkb" to BOM + body)).getValue("bin_pkg.pkb")
        val without = issues(mapOf("bin_pkg.pkb" to body)).getValue("bin_pkg.pkb")
        assertThat(withMark).isNotEmpty.isEqualTo(without)
    }

    @Test
    fun aSpecWithAByteOrderMarkResolvesTheCallsOfItsBody() {
        val spec = """
            create or replace package bin_api is
              procedure note(p varchar2);
            end;
        """.trimIndent()
        val caller = """
            create or replace procedure bin_caller is
            begin
              bin_api.note('x');
            end;
        """.trimIndent()
        val withMark = bindings(mapOf("bin_api.pks" to BOM + spec, "bin_caller.prc" to caller)).getValue("bin_caller.prc")
        val without = bindings(mapOf("bin_api.pks" to spec, "bin_caller.prc" to caller)).getValue("bin_caller.prc")
        assertThat(without).singleElement().asString().contains("RESOLVED")
        assertThat(withMark).isEqualTo(without)
    }

    @Test
    fun aFileWithOnlyAByteOrderMarkParses() {
        PlSqlParser.create(configuration).parse(BOM)
    }

    private fun scanner(sources: Map<String, String>): AstScanner {
        val preparation = ProjectIndexPreparation(ProjectDeclarationExtractor(configuration))
            .prepare(sources.map { (name, text) -> ProjectSource(FileId(name)) { text } }, concurrent = false)
        return AstScanner(listOf(UnusedParameterCheck()), null, false, StandardCharsets.UTF_8, ProjectAnalysisContext.prepared(preparation))
    }

    private fun issues(sources: Map<String, String>): Map<String, List<String>> {
        val scanner = scanner(sources)
        return sources.mapValues { (name, text) ->
            scanner.scanFile(BinFile(name, text), fileId = FileId(name)).issues.map {
                val at = it.primaryLocation
                "${at.startLine()}:${at.startLineOffset()}-${at.endLine()}:${at.endLineOffset()} ${at.message()}"
            }
        }
    }

    private fun bindings(sources: Map<String, String>): Map<String, List<String>> {
        val scanner = scanner(sources)
        return sources.mapValues { (name, text) ->
            val visitor = CallBindings()
            scanner.scanFile(BinFile(name, text), listOf(visitor), FileId(name))
            visitor.found
        }
    }

    private class CallBindings : PlSqlCheck() {
        val found = ArrayList<String>()

        override fun init() {
            subscribeTo(PlSqlGrammar.CALL_STATEMENT)
        }

        override fun visitNode(node: AstNode) {
            val call = node.firstChild
            found += context.callBinding(call.firstChild).toString()
        }
    }

    private class BinFile(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private companion object {
        const val BOM = "\uFEFF"
    }
}
