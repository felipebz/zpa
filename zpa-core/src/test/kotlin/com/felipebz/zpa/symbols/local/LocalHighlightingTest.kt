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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.HighlightingRange
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** The highlighting pairs the scanner gives from the local resolution of a file. */
class LocalHighlightingTest {

    private class File(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** Each pair as `name@line:column <- name@line:column, …`, for the file [name] of [sources]. */
    private fun pairs(sources: Map<String, String>, name: String = sources.keys.first(), prepared: Boolean = true): List<String> {
        val project = if (!prepared) ProjectAnalysisContext.NOT_PREPARED else ProjectAnalysisContext.prepared(
            ProjectIndexPreparation(ProjectDeclarationExtractor(PlSqlConfiguration(StandardCharsets.UTF_8)))
                .prepare(sources.map { (file, text) -> ProjectSource(FileId(file)) { text } }, concurrent = false)
        )
        val text = sources.getValue(name)
        val lines = text.split("\n")
        fun spell(range: HighlightingRange) = "${lines[range.startLine - 1].substring(range.startColumn, range.endColumn)}@${range.startLine}:${range.startColumn}"
        val result = AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, project).scanFile(File(name, text), emptyList(), FileId(name))
        return result.highlightingPairs.map { pair -> "${spell(pair.declaration)} <- ${pair.references.joinToString { spell(it) }}" }
    }

    private fun pairs(source: String, prepared: Boolean = true) = pairs(mapOf("a.sql" to source), prepared = prepared)

    @Test
    fun aCertainBindingIsAReference() {
        assertThat(pairs("declare\n  v number;\nbegin\n  v := 1;\nend;\n/\n")).containsExactly("v@2:2 <- v@4:2")
    }

    @Test
    fun eachSegmentOfAQualifiedNameIsItsOwnReference() {
        assertThat(pairs("<<b>>\ndeclare\n  v number;\nbegin\n  b.v := 1;\nend;\n/\n"))
            .containsExactly("b@1:2 <- b@5:2", "v@3:2 <- v@5:4")
    }

    @Test
    fun anUncertainNameWithAProvisionalDeclarationIsAReference() {
        // The conditional declarations make the block incomplete: v is provisionally the block's, z has no candidate.
        val source = "declare\n  v number;\n  \$if \$\$x \$then w number; \$end\nbegin\n  v := 1;\n  z := 1;\nend;\n/\n"
        assertThat(pairs(source)).containsExactly("v@2:2 <- v@5:2", "w@3:16 <- ")
    }

    @Test
    fun aCursorVariableInOpenFetchAndCloseIsAReference() {
        val source = "declare\n  c sys_refcursor;\n  x number;\nbegin\n  open c for select 1 from dual;\n  fetch c into x;\n  close c;\nend;\n/\n"
        assertThat(pairs(source)).containsExactly("c@2:2 <- c@5:7, c@6:8, c@7:8", "x@3:2 <- x@6:15")
    }

    @Test
    fun aCursorParameterInOpenFetchAndCloseIsAReference() {
        val source = "create procedure p(c in out sys_refcursor, x out number) is\nbegin\n  open c for select 1 from dual;\n" +
            "  fetch c into x;\n  close c;\nend;\n/\n"
        assertThat(pairs(source)).containsExactly("p@1:17 <- ", "c@1:19 <- c@3:7, c@4:8, c@5:8", "x@1:43 <- x@4:15")
    }

    @Test
    fun aMemberPositionIsNeverAReference() {
        val source = "create procedure ret_name is begin null; end;\n/\nCALL warehouse_typ(1).ret_name() INTO :x;\n"
        assertThat(pairs(source)).containsExactly("ret_name@1:17 <- ")
        val element = "declare\n  type t is table of number;\n  a t;\n  n number;\nbegin\n  x := a(1).n;\nend;\n/\n"
        assertThat(pairs(element)).containsExactly("t@2:7 <- t@3:4", "a@3:2 <- a@6:7", "n@4:2 <- ")
    }

    @Test
    fun theStarOfARelationIsNeverAReference() {
        val source = "declare\n  d1 number;\nbegin\n  for r in (select d1.* from dual d1) loop null; end loop;\nend;\n/\n"
        assertThat(pairs(source).joinToString()).doesNotContain("*@")
    }

    @Test
    fun aTypeNameRefersToTheTypeNeverToItsConstructor() {
        val source = "create type t as object (n number, constructor function t return self as result);\n/\n" +
            "create type body t as\n  constructor function t return self as result is\n    v t;\n  begin\n    return;\n  end;\nend;\n/\n"
        val pairs = pairs(source)
        assertThat(pairs.single { it.startsWith("t@4:23") }).isEqualTo("t@4:23 <- ")
        assertThat(pairs.single { it.startsWith("t@1:12") }).contains("t@5:6")
    }

    @Test
    fun aSchemaObjectCreatedEarlierInTheFileIsReferredToWhenTheProjectHoldsIt() {
        val source = "create type t_csv_line as object (s varchar2(10));\n/\ncreate type t_csv_tab as table of t_csv_line;\n/\n"
        assertThat(pairs(source)).contains("t_csv_line@1:12 <- t_csv_line@3:34")
        val function = "create function f return number is begin return 1; end;\n/\ncreate table t as select f() x from dual;\n"
        assertThat(pairs(function)).contains("f@1:16 <- f@3:25")
        // Without a project the file alone proves nothing about the schema.
        assertThat(pairs(source, prepared = false)).contains("t_csv_line@1:12 <- ")
    }

    @Test
    fun aPackageBodyNameRefersToItsSpecificationInTheFile() {
        val source = "create package p is\n  procedure x;\nend;\n/\ncreate package body p is\n  procedure x is begin null; end;\nend;\n/\n"
        val pairs = pairs(source)
        assertThat(pairs.single { it.startsWith("p@") }).isEqualTo("p@1:15 <- p@5:20")
    }

    @Test
    fun aDeclarationInAnotherFileIsNeverPaired() {
        val sources = mapOf(
            "use.sql" to "begin\n  pkg.v := 1;\nend;\n/\n",
            "pkg.sql" to "create package pkg is\n  v number;\nend;\n/\n"
        )
        assertThat(pairs(sources)).isEmpty()
    }
}
