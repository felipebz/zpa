//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(ZpaExperimentalApi::class)
class ProjectPackageExceptionCursorViewTest {
    private fun prepare(vararg files: Pair<String, String>): ProjectSnapshot =
        ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) }, false)

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)

    private val errorPkg = """
        create or replace package error_pkg is
          app_error exception;
          pragma exception_init(app_error, -20999);
          cursor c_pending(p_status number) return send_queue%rowtype;
          cursor c_all is select id from send_queue;
        end;
        /
    """.trimIndent()

    @Test
    fun specificationViewsListExceptionsAndCursors() {
        val s = prepare("error_pkg.pks" to errorPkg)
        val members = s.declarationsFor("error_pkg.pks").declarations.drop(1)
        assertThat(members.map { it.kind }).containsExactly(
            ProjectDeclarationKind.PACKAGE_EXCEPTION, ProjectDeclarationKind.PACKAGE_CURSOR, ProjectDeclarationKind.PACKAGE_CURSOR
        )
        assertThat(members.map { it.role }).containsOnly(ProjectDeclarationRole.SPECIFICATION)

        val exception = members[0] as ProjectPackageExceptionView
        assertThat(exception.owner).isEqualTo(n("error_pkg"))
        assertThat(exception.localName).isEqualTo(ProjectIdentifier.fromSource("APP_ERROR"))
        assertThat(exception.qualifiedName).isEqualTo(n("error_pkg", "app_error"))
        assertThat(exception.sourceRange.startLine).isEqualTo(2)

        val pending = members[1] as ProjectPackageCursorView
        assertThat(pending.owner).isEqualTo(n("error_pkg"))
        assertThat(pending.localName.originalSpelling).isEqualTo("c_pending")
        assertThat(pending.parameters.map { "${it.ordinal} ${it.name} ${it.mode} ${it.typeRef.name}" }).containsExactly("1 p_status IN number")
        val returnType = pending.returnType.get()
        assertThat(returnType.kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
        assertThat(returnType.anchor).contains(ProjectTypeReference.Anchor.ROWTYPE)
        assertThat(returnType.name).isEqualTo(n("send_queue"))

        val all = members[2] as ProjectPackageCursorView
        assertThat(all.parameters).isEmpty()
        assertThat(all.returnType).isEmpty
        assertThat(s.findDeclarations(n("ERROR_PKG", "C_ALL")).declarations.map { it.id }).containsExactly(all.id)
    }

    @Test
    fun valueResolutionListsButNeverResolvesExceptionsOrCursors() {
        val s = prepare("error_pkg.pks" to errorPkg)
        val exception = s.resolvePackageValue(n("error_pkg", "app_error"), null)
        assertThat(exception.status).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT)
        assertThat(exception.value).isEmpty
        assertThat(exception.candidates.single()).isInstanceOf(ProjectPackageExceptionView::class.java)
        val cursor = s.resolvePackageValue(n("error_pkg", "c_all"), null)
        assertThat(cursor.status).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT)
        assertThat(cursor.candidates.single()).isInstanceOf(ProjectPackageCursorView::class.java)
    }

    @Test
    fun aCursorOfAnotherPackageDoesNotChangeAVariableResolution() {
        val s = prepare(
            "a.pks" to "CREATE PACKAGE a AS g NUMBER; END a;",
            "b.pks" to "CREATE PACKAGE b AS CURSOR g IS SELECT 1 FROM dual; END b;"
        )
        val g = s.resolvePackageValue(n("a", "g"), null)
        assertThat(g.status).isEqualTo(ProjectValueResolution.Status.RESOLVED)
        assertThat(g.candidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_VARIABLE)
        assertThat(s.resolvePackageValue(n("b", "g"), null).status).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun duplicateExceptionsInTwoFilesAreTwoOccurrencesInStableOrder() {
        val files = arrayOf("b.pks" to "CREATE PACKAGE p AS e EXCEPTION; END p;", "a.pks" to "CREATE PACKAGE p AS e EXCEPTION; END p;")
        val first = prepare(*files).findDeclarations(n("p", "e")).declarations
        val second = prepare(*files.reversedArray()).findDeclarations(n("p", "e")).declarations
        assertThat(first.map { it.fileId }).containsExactly("a.pks", "b.pks")
        assertThat(second.map { it.fileId }).containsExactly("a.pks", "b.pks")
        assertThat(first.map { it.id }).doesNotHaveDuplicates()
    }

    @Test
    fun cursorAndExceptionAnchorsAreSourceCandidates() {
        val s = prepare("error_pkg.pks" to errorPkg)
        val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("APP").build()
        fun describe(owner: ProjectQualifiedName?, vararg name: String) = s.describeExternalType(
            ProjectWrittenAnchor.of(n(*name), ProjectTypeReference.Anchor.ROWTYPE), owner, metadata, ProjectIdentifier.fromSource("APP")
        )
        // Inside the package, the member captures the anchor: never an external relation, and the member is the evidence.
        val inside = describe(n("error_pkg"), "c_all")
        assertThat(inside.status).isEqualTo(ProjectExternalTypeDescription.Status.NOT_EXTERNAL)
        assertThat(inside.sourceCandidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_CURSOR)
        assertThat(describe(n("error_pkg"), "app_error").sourceCandidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_EXCEPTION)
        // Qualified from outside, the project does not prove the package is in the current schema, and a public synonym
        // the metadata does not hold may take the qualifier: the reading stays open, with the package and its cursor
        // listed as possible captors.
        val outside = describe(null, "error_pkg", "c_all")
        assertThat(outside.status).isEqualTo(ProjectExternalTypeDescription.Status.CAPTURE_NOT_EXCLUDED)
        assertThat(outside.sourceCandidates.map { it.kind })
            .containsExactly(ProjectDeclarationKind.PACKAGE, ProjectDeclarationKind.PACKAGE_CURSOR)
    }

    private fun cursor(declaration: String): ProjectPackageCursorView {
        val s = prepare("shop_pkg.pks" to "create or replace package shop_pkg is\n$declaration\nend;\n/")
        return s.declarationsFor("shop_pkg.pks").declarations.filterIsInstance<ProjectPackageCursorView>().single()
    }

    private fun names(declaration: String): List<String> =
        cursor(declaration).selectList.get().map { it.name.map { n -> n.originalSpelling }.orElse("?") }

    @Test
    fun columnItemsAreNamedByTheirLastSegmentInOrder() {
        val c = cursor("cursor c is select l.line_no, sysdate, s.o.amount from order_lines l;")
        val items = c.selectList.get()
        assertThat(items.map { it.ordinal }).containsExactly(0, 1, 2)
        assertThat(items.map { it.name.get().originalSpelling }).containsExactly("line_no", "sysdate", "amount")
        assertThat(items[0].name.get()).isEqualTo(ProjectIdentifier.fromSource("LINE_NO"))
        val first = items[0].sourceRange
        assertThat(listOf(first.startLine, first.startColumn, first.endColumn)).containsExactly(2, 21, 28)
        assertThat(items[1].sourceRange.startColumn).isEqualTo(30)
    }

    @Test
    fun aliasesNameTheField() {
        assertThat(names("cursor c is select l.qty as amount, l.qty * l.price total, l.price + 1 \"Mixed Case\" from order_lines l;"))
            .containsExactly("amount", "total", "\"Mixed Case\"")
        assertThat(names("cursor c is select case when l.qty > 0 then 1 else 0 end has_qty, count(*) n, 'x' tag from order_lines l;"))
            .containsExactly("has_qty", "n", "tag")
    }

    @Test
    fun starsAndExpressionsWithoutAliasHaveNoName() {
        val c = cursor("cursor c is select *, l.*, l.price + 1, nvl(l.qty, 0) from order_lines l;")
        val items = c.selectList.get()
        assertThat(items.map { it.name.isPresent }).containsExactly(false, false, false, false)
        assertThat(items[1].sourceRange.let { it.endColumn - it.startColumn }).isEqualTo(3)
    }

    @Test
    fun aKeywordAfterAnExpressionIsNoAlias() {
        // Oracle rejects r.day, r.local, r.month, r.multiset and r.binary_ci: these items have no alias, and Oracle names
        // each one by its text.
        assertThat(names(
            "cursor c_items is select interval '1' day, interval '1-2' year to month, l.created at time zone 'UTC', l.created at local, " +
                "date '2020-01-01', timestamp '2020-01-01 00:00:00', l.qty from order_line l;"
        )).containsExactly("?", "?", "?", "?", "?", "?", "qty")
        assertThat(names(
            "cursor c_more is select l.v multiset union l.w, l.v multiset except distinct l.w, l.name collate binary_ci, " +
                "max(l.qty) keep (dense_rank first order by l.order_id) over (partition by l.order_id), sum(l.qty) over (partition by l.order_id), " +
                "listagg(l.name, ',') within group (order by l.name), q'[it's]', prior l.qty from order_line l;"
        )).containsExactly("?", "?", "?", "?", "?", "?", "?", "?")
    }

    @Test
    fun anAliasAfterSuchAnExpressionStillNamesIt() {
        // An alias written after such an expression names the field (Oracle accepts r.rn, r.d and r.t).
        assertThat(names(
            "cursor c is select rownum rn, date '2020-01-01' d, l.created at local as t, l.name collate binary_ci nm, " +
                "interval '1' day as one_day from order_line l;"
        )).containsExactly("rn", "d", "t", "nm", "one_day")
        // An alias after a keyword the expression may end with is not read: the item has no known name.
        assertThat(names("cursor c is select interval '1' day one_day from order_line l;")).containsExactly("?")
    }

    @Test
    fun distinctWithAndUnionTakeTheMainFirstBlock() {
        assertThat(names("cursor c is select distinct l.line_no from order_lines l;")).containsExactly("line_no")
        assertThat(names("cursor c is with q as (select a, b from order_lines) select q.a first_col, q.b from q;"))
            .containsExactly("first_col", "b")
        assertThat(names("cursor c is (select l.line_no x from order_lines l) union select 1 y from dual order by 1;"))
            .containsExactly("x")
        assertThat(names("cursor c is select 1 one;")).containsExactly("one")
    }

    @Test
    fun returnClauseAloneHasNoSelectListAndWithQueryHasBoth() {
        assertThat(cursor("cursor c return order_lines%rowtype;").selectList).isEmpty
        val both = cursor("cursor c return order_lines%rowtype is select l.line_no from order_lines l;")
        assertThat(both.returnType).isPresent
        assertThat(both.selectList.get().map { it.name.get().originalSpelling }).containsExactly("line_no")
    }

    @Test
    fun anUnreadableQueryGivesOneUnnamedItemCoveringTheQuery() {
        val c = cursor("cursor c is select a,, b from order_lines;")
        val items = c.selectList.get()
        assertThat(items).hasSize(1)
        assertThat(items[0].name).isEmpty
        val range = items[0].sourceRange
        assertThat(listOf(range.startLine, range.startColumn, range.endColumn)).containsExactly(2, 12, 41)
    }
}
