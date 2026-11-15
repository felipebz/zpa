//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
        val result = AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, project, highlightingPairs = true).scanFile(File(name, text), emptyList(), FileId(name))
        return result.highlightingPairs.map { pair -> "${spell(pair.declaration)} <- ${pair.references.joinToString { spell(it) }}" }
    }

    private fun pairs(source: String, prepared: Boolean = true) = pairs(mapOf("a.sql" to source), prepared = prepared)

    @Test
    fun lineLengthsSplitLinesAsEachLineBreakEndsThem() {
        val texts = listOf(
            "", "abc", "a\nbc", "a\r\nbc", "a\rbc", "a\r\n\nb\rc\n\r\nd", "\r\r\n\n",
            "ab\n", "ab\r\n", "ab\r", "a\n\r", "\n"
        ) + listOf(15, 16, 17, 32, 33).flatMap { n -> listOf("a\n".repeat(n), "a\r\n".repeat(n) + "tail") }
        for (text in texts) {
            assertThat(LocalHighlighting.lineLengths(text))
                .`as`(text.replace("\r", "\\r").replace("\n", "\\n"))
                .containsExactly(*text.split("\r\n", "\n", "\r").map { it.length }.toIntArray())
        }
    }

    @Test
    fun aScannerComputesThePairsOnlyWhenAskedFor() {
        val text = "declare\n  v_total number;\nbegin\n  v_total := v_total + 1;\nend;\n/\n"
        fun scan(asked: Boolean) = AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, highlightingPairs = asked)
            .scanFile(File("a.sql", text), emptyList(), FileId("a.sql")).highlightingPairs
        assertThat(scan(asked = false)).isEmpty()
        assertThat(scan(asked = true)).hasSize(1).first().satisfies({ assertThat(it.references).hasSize(2) })
    }

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

    /** The pairs of [statements], written one per line in a block that declares `v`, without the pair of `v`. */
    private fun sqlPairs(vararg statements: String): List<String> =
        pairs("declare\n  v number;\nbegin\n" + statements.joinToString("") { "  $it\n" } + "end;\n").filterNot { it.startsWith("v@") }

    @Test
    fun aRelationWithoutAliasIsDeclaredByItsName() {
        assertThat(sqlPairs("select dept.id into v from dept;")).containsExactly("dept@4:29 <- dept@4:9")
    }

    @Test
    fun anAliasIsDeclaredOnceAndEveryQualifierRefersToIt() {
        assertThat(sqlPairs("select w.id, w.label into v, v from ward w where w.id = 1 order by w.label;"))
            .containsExactly("w@4:43 <- w@4:9, w@4:15, w@4:51, w@4:69")
    }

    @Test
    fun theRelationSegmentOfAThreePartNameRefersToTheRelation() {
        assertThat(sqlPairs("select clinic.ward.id into v from clinic.ward;")).containsExactly("ward@4:43 <- ward@4:16")
    }

    @Test
    fun theAliasIsTheReferenceWhateverFollowsTheColumn() {
        // An attribute of an object column, a method called on a column, an attribute of an inline view's column.
        assertThat(sqlPairs("select w.loc.city into v from ward w;")).containsExactly("w@4:37 <- w@4:9")
        assertThat(sqlPairs("select w.doc.getclobval() into v from ward w;")).containsExactly("w@4:45 <- w@4:9")
        assertThat(sqlPairs("select s.item.label into v from (select value(t) item from table(f_beds) t) s;"))
            .contains("s@4:78 <- s@4:9")
    }

    @Test
    fun theColumnsOfAMultiColumnSetReferToTheTarget() {
        assertThat(sqlPairs("update ward w set (w.label, w.floor) = (select 'x', 1 from dual) where w.id = 1;"))
            .containsExactly("w@4:14 <- w@4:21, w@4:30, w@4:73")
    }

    @Test
    fun aColumnOfAnInsertListRefersToTheTarget() {
        assertThat(sqlPairs("insert into ward w (w.id, w.label) values (1, 'x');")).containsExactly("w@4:19 <- w@4:22, w@4:28")
    }

    @Test
    fun theQualifierOfAStarAndTheArgumentOfRefReferToTheSource() {
        assertThat(pairs("begin\n  for r in (select k.* from kennel k) loop null; end loop;\nend;\n")).contains("k@2:35 <- k@2:19")
        assertThat(sqlPairs("select ref(o) into v from hutch_objs o;")).containsExactly("o@4:39 <- o@4:13")
    }

    @Test
    fun anInlineViewIsDeclaredByItsAlias() {
        assertThat(sqlPairs("select iv.n into v from (select 1 n from dual) iv;")).containsExactly("iv@4:49 <- iv@4:9")
    }

    @Test
    fun eachAliasOfAJoinHasItsOwnReferences() {
        assertThat(sqlPairs("select a.id into v from ward a join bed b on a.id = b.ward_id where b.free = 'Y';"))
            .containsExactly("a@4:31 <- a@4:9, a@4:47", "b@4:42 <- b@4:54, b@4:70")
    }

    @Test
    fun aCorrelatedSubqueryReadsTheOuterAlias() {
        assertThat(sqlPairs("select w.id into v from ward w where exists (select 1 from bed b where b.ward_id = w.id);"))
            .containsExactly("w@4:31 <- w@4:9, w@4:85", "b@4:65 <- b@4:73")
    }

    @Test
    fun theSameAliasInTwoStatementsIsTwoSymbols() {
        assertThat(sqlPairs("select w.id into v from ward w;", "select w.id into v from bed w;"))
            .containsExactly("w@4:31 <- w@4:9", "w@5:30 <- w@5:9")
    }

    @Test
    fun theTargetsOfUpdateDeleteAndMergeAreDeclaredByTheirAliases() {
        assertThat(sqlPairs(
            "update ward w set w.label = 'x' where w.id = 2;",
            "delete from bed b where b.id = 3;",
            "merge into ward t using bed s on (t.id = s.ward_id) when matched then update set t.label = s.label;"
        )).containsExactly(
            "w@4:14 <- w@4:20, w@4:40",
            "b@5:18 <- b@5:26",
            "t@6:18 <- t@6:36, t@6:83",
            "s@6:30 <- s@6:43, s@6:93"
        )
    }

    @Test
    fun aQualifierThatIsAlsoARecordGoesToTheSourceInsideTheStatementOnly() {
        val text = "declare\n  type row_t is record (id number);\n  q row_t;\nbegin\n" +
            "  select q.id into q.id from (select 1 id from dual) q;\n  q.id := 2;\nend;\n"
        assertThat(pairs(text)).contains("q@3:2 <- q@5:19, q@6:2", "q@5:53 <- q@5:9")
    }

    @Test
    fun anUnqualifiedColumnOrAnUndecidedQualifierPairsNothing() {
        assertThat(sqlPairs("select id into v from ward;")).isEmpty()
        // The loop record or the column of the alias may take `l.qty`: the alias stays unpaired.
        assertThat(sqlPairs("for l in (select 7 qty from dual) loop select l.qty into v from bed l; end loop;"))
            .noneMatch { it.startsWith("l@4:66") }
    }
}
