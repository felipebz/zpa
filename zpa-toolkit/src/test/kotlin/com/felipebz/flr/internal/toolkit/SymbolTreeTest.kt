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

package com.felipebz.flr.internal.toolkit

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.PlSqlConfiguration
import com.felipebz.zpa.tooling.SemanticInspectionService
import com.felipebz.zpa.toolkit.ZpaConfigurationModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import javax.swing.tree.DefaultMutableTreeNode

class SymbolTreeTest {

    /** The tab's tree, as the view builds it, one node per line indented by depth. */
    private fun tab(source: String): String {
        val tree = SourceCodeModel(ZpaConfigurationModel()).apply { setSourceCode(source) }.symbolTree!!
        val root = ToolkitViewImpl.toSymbolTreeNode(tree)
        return root.preorderEnumeration().asSequence().map { it as DefaultMutableTreeNode }
            .joinToString("\n") { "  ".repeat(it.level) + it.userObject }
    }

    @Test
    fun theTabShowsRegionsDeclarationsAndReferences() {
        val source = """
            |<<outer>>
            |declare
            |  v number;
            |  c sys_refcursor;
            |  type r is record (n number);
            |  type t is table of r;
            |  a t;
            |  n number;
            |begin
            |  declare
            |    w number;
            |  begin
            |    outer.v := w;
            |  end;
            |  open c for select 1 from dual;
            |  n := a(1).n;
            |end;
            |/
            |""".trimMargin()
        assertThat(tab(source)).isEqualTo("""
            |FILE (lines 1-18)
            |  LABEL outer (1:2)
            |    13:4
            |  ANONYMOUS_BLOCK (lines 2-17)
            |    VARIABLE v (3:2)
            |      13:10
            |    VARIABLE c (4:2)
            |      15:7
            |    RECORD_TYPE r (5:7)
            |      6:21
            |    COLLECTION_TYPE t (6:7)
            |      7:4
            |    VARIABLE a (7:2)
            |      16:7
            |    VARIABLE n (8:2)
            |      16:2
            |    ANONYMOUS_BLOCK (lines 10-14)
            |      VARIABLE w (11:4)
            |        13:15
            """.trimMargin())
    }

    @Test
    fun theToolkitPairsAreTheScannerPairs() {
        val text = File("../sonar-zpa-plugin/src/test/resources/com/felipebz/zpa/symbols/highlighting.sql").readText()
        val fileId = FileId("highlighting.sql")
        fun project() = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation(ProjectDeclarationExtractor(PlSqlConfiguration(StandardCharsets.UTF_8)))
                .prepare(listOf(ProjectSource(fileId) { text }), concurrent = false)
        )
        val root = ZpaConfigurationModel().parser.parse(text)
        val toolkit = SemanticInspectionService().analyze(root, fileId, project(), text).highlightingPairs
        val file = object : PlSqlFile {
            override fun contents() = text
            override fun fileName() = "highlighting.sql"
            override fun path(): Path = Path.of("highlighting.sql")
            override fun type() = PlSqlFile.Type.MAIN
        }
        val scanner = AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, project()).scanFile(file, emptyList(), fileId).highlightingPairs
        assertThat(toolkit).isNotEmpty().isEqualTo(scanner)
    }
}
