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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalTypeDescription.Status
import com.felipebz.zpa.api.project.ProjectTypeReference.Anchor.ROWTYPE
import com.felipebz.zpa.api.project.ProjectTypeReference.Anchor.TYPE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.Executors
import com.felipebz.zpa.api.project.ProjectExternalLookup.Status as Lookup
import com.felipebz.zpa.api.project.ProjectTypeDescription.Status as Source

/** Source/external composition through public APIs; the decision order follows behavior verified on Oracle. */
@OptIn(ZpaExperimentalApi::class)
class ProjectExternalTypeDescriptionTest {
    private val idColumn = ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 8, 0))
    private val salaryColumn = ProjectExternalColumn.of("SALARY", 2, ProjectExternalDatatype.of("NUMBER", null, null, null, 10, 2))
    private val nameColumn = ProjectExternalColumn.of("NAME", 3, ProjectExternalDatatype.of("VARCHAR2", null, 30, ProjectExternalDatatype.LengthSemantics.BYTE, null, null))
    private val employees = ProjectExternalRelation.of("HR", "EMPLOYEES", true, listOf(nameColumn, idColumn, salaryColumn))
    private val md = metadata(employees)
    private val hr = ProjectIdentifier.fromSource("HR")
    private val app = ProjectIdentifier.fromSource("APP")

    private fun metadata(vararg relations: ProjectExternalRelation, complete: List<String> = listOf("HR")): ProjectExternalMetadata {
        val builder = ProjectExternalMetadata.builder("fixture")
        complete.forEach { builder.completeSchema(it) }
        relations.forEach { builder.relation(it) }
        return builder.build()
    }

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)
    private fun w(kind: ProjectTypeReference.Anchor, vararg s: String) = ProjectWrittenAnchor.of(n(*s), kind)
    private fun prepare(vararg files: Pair<String, String>, failing: Boolean = false) = ProjectSnapshots.prepare(
        files.map { (id, text) -> ProjectSourceInput.ofText(id, text) } +
            (if (failing) listOf(ProjectSourceInput.ofReader("bad.sql", ProjectSourceReader { throw IllegalStateException("x") })) else emptyList()),
        false
    )
    private val empty = prepare()

    private fun single(s: ProjectSnapshot, vararg name: String) = s.findDeclarations(n(*name)).declarations.single()
    private fun element(s: ProjectSnapshot, vararg name: String) = (single(s, *name) as ProjectShapedTypeView).elementType.get()
    private fun valueRef(s: ProjectSnapshot, vararg name: String) = (single(s, *name) as ProjectPackageValueView).typeRef
    private fun names(views: List<ProjectDeclarationView>) = views.map { it.qualifiedName.toString() }
    private fun fields(d: ProjectExternalTypeDescription) = d.lookup.get().relation.get().rowFields.map { it.name.toString() }

    // Written anchors of an anonymous block, never indexed.

    @Test
    fun writtenColumnAndRowAnchorsResolveToTheExternalRelation() {
        val sal = empty.describeExternalType(w(TYPE, "employees", "salary"), null, md, hr)
        assertThat(sal.status).isEqualTo(Status.RESOLVED)
        assertThat(sal.source.status).isEqualTo(Source.UNRESOLVED_VALUE)
        assertThat(sal.source.valueResolution.get().status).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT)
        assertThat(sal.source.steps).isEmpty()
        assertThat(sal.source.terminalReference).isEmpty
        assertThat(sal.source.writtenAnchor).contains(w(TYPE, "employees", "salary"))
        assertThat(sal.anchor).contains(TYPE)
        assertThat(sal.anchorName).contains(n("employees", "salary"))
        assertThat(sal.relationName).contains(n("HR", "EMPLOYEES"))
        val column = sal.lookup.get().column.get()
        assertThat(sal.lookup.get().status).isEqualTo(Lookup.FOUND)
        assertThat(column.name.toString()).isEqualTo("\"SALARY\"")
        assertThat(column.position).isEqualTo(OptionalInt.of(2))
        val type = column.datatype.get()
        assertThat(type.precision).isEqualTo(OptionalInt.of(10))
        assertThat(type.scale).isEqualTo(OptionalInt.of(2))
        assertThat(type.length).isEmpty
        assertThat(type.category).contains(ProjectBuiltInTypeCategory.NUMERIC)
        assertThat(sal.lookup.get().origin).contains("fixture")
        assertThat(sal.sourceCandidates).isEmpty()
        assertThat(sal.externalCandidates).isEmpty()

        val row = empty.describeExternalType(w(ROWTYPE, "employees"), null, md, hr)
        assertThat(row.status).isEqualTo(Status.RESOLVED)
        assertThat(row.source.status).isEqualTo(Source.ANCHORED_REFERENCE)
        assertThat(row.source.anchorStop).contains(ProjectTypeDescription.AnchorStop.ROWTYPE)
        assertThat(row.source.terminalReference).isEmpty
        assertThat(fields(row)).containsExactly("\"ID\"", "\"SALARY\"", "\"NAME\"")
        assertThat(row.lookup.get().relation.get().rowObjectType).isEmpty
        // No external fact becomes a project declaration.
        assertThat(empty.declarations).isEmpty()
        assertThat(empty.findDeclarations(n("employees")).declarations).isEmpty()
    }

    @Test
    fun externalCoverageDecidesAbsenceOnlyWithinItsContract() {
        val sal = w(TYPE, "employees", "salary")
        val row = w(ROWTYPE, "employees")
        fun both(m: ProjectExternalMetadata, schema: ProjectIdentifier? = hr) =
            listOf(empty.describeExternalType(sal, null, m, schema), empty.describeExternalType(row, null, m, schema))

        val none = both(ProjectExternalMetadata.none())
        assertThat(none.map { it.status }).containsExactly(Status.UNAVAILABLE, Status.UNAVAILABLE)
        assertThat(none[0].lookup.get().status).isEqualTo(Lookup.UNAVAILABLE)
        assertThat(none[0].source.status).isEqualTo(Source.UNRESOLVED_VALUE)

        val noSalary = both(metadata(ProjectExternalRelation.of("HR", "EMPLOYEES", true, listOf(idColumn, nameColumn))))
        assertThat(noSalary.map { it.status }).containsExactly(Status.COLUMN_NOT_FOUND, Status.RESOLVED)
        assertThat(noSalary[0].lookup.get().relation).isPresent
        assertThat(noSalary[0].source.valueResolution.get().status).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT)
        assertThat(fields(noSalary[1])).hasSize(2)

        assertThat(both(metadata()).map { it.status }).containsExactly(Status.RELATION_NOT_FOUND, Status.RELATION_NOT_FOUND)
        val partial = metadata(ProjectExternalRelation.of("HR", "EMPLOYEES", false, listOf(idColumn)), complete = emptyList())
        assertThat(both(partial).map { it.status }).containsExactly(Status.INCOMPLETE, Status.INCOMPLETE)
        assertThat(both(metadata(complete = emptyList())).map { it.status }).containsExactly(Status.INCOMPLETE, Status.INCOMPLETE)
        val noContext = both(md, null)
        assertThat(noContext.map { it.status }).containsExactly(Status.SCHEMA_CONTEXT_MISSING, Status.SCHEMA_CONTEXT_MISSING)
        assertThat(noContext[0].relationName).isEmpty
        assertThat(noContext[0].lookup).isEmpty
    }

    @Test
    fun columnStatesFollowTheDatatypeAndVisibilityContract() {
        val inv = ProjectExternalColumn.of("INV", null, ProjectExternalDatatype.of("DATE", null, null, null, null, null))
        val untyped = ProjectExternalColumn.of("NOTE", 4, null)
        val xml = ProjectExternalColumn.of("DOC", 5, ProjectExternalDatatype.of("XMLTYPE", "PUBLIC", null, null, null, null))
        val m = metadata(ProjectExternalRelation.of("HR", "EMPLOYEES", true, listOf(idColumn, salaryColumn, nameColumn, inv, untyped, xml)))
        val invisible = empty.describeExternalType(w(TYPE, "employees", "inv"), null, m, hr)
        assertThat(invisible.status).isEqualTo(Status.RESOLVED)
        assertThat(invisible.lookup.get().column.get().position).isEmpty
        assertThat(fields(empty.describeExternalType(w(ROWTYPE, "employees"), null, m, hr)))
            .containsExactly("\"ID\"", "\"SALARY\"", "\"NAME\"", "\"NOTE\"", "\"DOC\"")
        val unknown = empty.describeExternalType(w(TYPE, "employees", "note"), null, m, hr)
        assertThat(unknown.status).isEqualTo(Status.RESOLVED)
        assertThat(unknown.lookup.get().column.get().datatype).isEmpty
        val uncategorized = empty.describeExternalType(w(TYPE, "employees", "doc"), null, m, hr)
        assertThat(uncategorized.status).isEqualTo(Status.RESOLVED)
        assertThat(uncategorized.lookup.get().column.get().datatype.get().category).isEmpty
        // A system-generated name is outside the column coverage contract.
        assertThat(empty.describeExternalType(w(TYPE, "employees", "sys_nc00007$"), null, m, hr).status).isEqualTo(Status.INCOMPLETE)
        assertThat(empty.describeExternalType(w(TYPE, "employees", "bonus"), null, m, hr).status).isEqualTo(Status.COLUMN_NOT_FOUND)
    }

    @Test
    fun objectTablesKeepTheirRowType() {
        val rowType = ProjectExternalDatatype.of("EMP_T", "HR", null, null, null, null)
        val attrs = listOf(ProjectExternalColumn.of("A", 1, null), ProjectExternalColumn.of("B", 2, null))
        val complete = empty.describeExternalType(w(ROWTYPE, "emp_ot"), null, metadata(ProjectExternalRelation.objectTable("HR", "EMP_OT", rowType, true, attrs)), hr)
        assertThat(complete.status).isEqualTo(Status.RESOLVED)
        assertThat(complete.lookup.get().relation.get().rowObjectType).contains(rowType)
        assertThat(fields(complete)).containsExactly("\"A\"", "\"B\"")
        val partial = empty.describeExternalType(w(ROWTYPE, "emp_ot"), null, metadata(ProjectExternalRelation.objectTable("HR", "EMP_OT", rowType, false, attrs)), hr)
        assertThat(partial.status).isEqualTo(Status.INCOMPLETE)
        assertThat(partial.lookup.get().status).isEqualTo(Lookup.FOUND)
    }

    @Test
    fun quotedIdentifiersFollowOracleIdentity() {
        val m = metadata(ProjectExternalRelation.of("HR", "Emp", true, listOf(idColumn)))
        assertThat(empty.describeExternalType(w(ROWTYPE, "\"Emp\""), null, m, hr).status).isEqualTo(Status.RESOLVED)
        assertThat(empty.describeExternalType(w(ROWTYPE, "emp"), null, m, hr).status).isEqualTo(Status.RELATION_NOT_FOUND)
        assertThat(empty.describeExternalType(w(TYPE, "\"Emp\"", "id"), null, m, hr).status).isEqualTo(Status.RESOLVED)
        assertThat(empty.describeExternalType(w(TYPE, "\"Emp\"", "\"id\""), null, m, hr).status).isEqualTo(Status.COLUMN_NOT_FOUND)
    }

    @Test
    fun projectDeclarationsNamedLikeTheRelationArePossibleCaptorsNotConflicts() {
        val s = prepare("e.sql" to "CREATE PACKAGE employees AS other NUMBER; END employees;")
        val sal = w(TYPE, "employees", "salary")
        val resolved = s.describeExternalType(sal, null, md, hr)
        assertThat(resolved.status).isEqualTo(Status.RESOLVED)
        assertThat(resolved.sourceCandidates).isEmpty()
        val unavailable = s.describeExternalType(sal, null, ProjectExternalMetadata.none(), hr)
        assertThat(unavailable.status).isEqualTo(Status.UNAVAILABLE)
        assertThat(names(unavailable.sourceCandidates)).containsExactly("employees")
        val row = s.describeExternalType(w(ROWTYPE, "employees"), null, ProjectExternalMetadata.none(), hr)
        assertThat(names(row.sourceCandidates)).containsExactly("employees")
        val absent = s.describeExternalType(sal, null, metadata(), hr)
        assertThat(absent.status).isEqualTo(Status.RELATION_NOT_FOUND)
        assertThat(names(absent.sourceCandidates)).containsExactly("employees")
    }

    @Test
    fun preparationFailuresMakeTheSourceIncompleteWithTheLookupAttached() {
        val s = prepare(failing = true)
        val sal = s.describeExternalType(w(TYPE, "employees", "salary"), null, md, hr)
        assertThat(sal.status).isEqualTo(Status.SOURCE_INCOMPLETE)
        assertThat(sal.lookup.get().status).isEqualTo(Lookup.FOUND)
        assertThat(s.describeExternalType(w(ROWTYPE, "employees"), null, md, hr).status).isEqualTo(Status.SOURCE_INCOMPLETE)
        assertThat(s.describeExternalType(w(ROWTYPE, "employees"), null, ProjectExternalMetadata.none(), hr).lookup).isEmpty
    }

    // Retained collection element references.

    private val pSpec = "CREATE PACKAGE p AS\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;\nEND p;"

    @Test
    fun retainedCollectionElementComposesWithTheExternalRow() {
        val s = prepare("p_spec.sql" to pSpec)
        val described = s.describeType(n("p", "rows_t"), null)
        assertThat(described.status).isEqualTo(Source.DECLARED_TYPE)
        val view = described.declaration.get() as ProjectShapedTypeView
        assertThat(view.fileId).isEqualTo("p_spec.sql")
        val elementRef = view.elementType.get()
        assertThat(elementRef.kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
        assertThat(elementRef.anchor).contains(ROWTYPE)
        val e = s.describeExternalType(elementRef, n("p"), md, hr)
        assertThat(e.status).isEqualTo(Status.RESOLVED)
        assertThat(e.source.status).isEqualTo(Source.ANCHORED_REFERENCE)
        assertThat(e.source.terminalReference.get().sourceRange).isEqualTo(elementRef.sourceRange)
        assertThat(e.source.terminalReference.get().sourceRange.fileId).isEqualTo("p_spec.sql")
        assertThat(e.source.writtenAnchor).isEmpty
        assertThat(fields(e)).containsExactly("\"ID\"", "\"SALARY\"", "\"NAME\"")
        assertThat(e.lookup.get().origin).contains("fixture")
        // The source collection keeps its identity and range; no external fact is attached to it.
        val again = s.describeType(n("p", "rows_t"), null).declaration.get()
        assertThat(again.id).isEqualTo(view.id)
        assertThat(again.sourceRange).isEqualTo(view.sourceRange)
        // Missing metadata does not invalidate the source declaration.
        assertThat(s.describeExternalType(elementRef, n("p"), ProjectExternalMetadata.none(), hr).status).isEqualTo(Status.UNAVAILABLE)
        assertThat(s.describeType(n("p", "rows_t"), null).status).isEqualTo(Source.DECLARED_TYPE)
    }

    private fun spec(body: String) = "CREATE PACKAGE p AS\n  TYPE r_t IS RECORD (only_field NUMBER);\n$body\nEND p;"
    private fun rows(source: String, m: ProjectExternalMetadata = md): ProjectExternalTypeDescription {
        val s = prepare("p.sql" to source)
        return s.describeExternalType(element(s, "p", "rows_t"), n("p"), m, hr)
    }

    @Test
    fun specificationMembersCaptureOrRejectByPosition() {
        val before = rows(spec("  CURSOR employees RETURN r_t;\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;"))
        assertThat(before.status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(before.lookup).isEmpty
        assertThat(before.anchor).contains(ROWTYPE)
        val after = rows(spec("  TYPE rows_t IS TABLE OF employees%ROWTYPE;\n  CURSOR employees RETURN r_t;"))
        assertThat(after.status).isEqualTo(Status.NOT_EXTERNAL)
        // A retained member that captures is listed.
        val variable = rows(spec("  employees r_t;\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;"))
        assertThat(variable.status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(names(variable.sourceCandidates)).containsExactly("p.employees")
    }

    @Test
    fun nameCoverageGapsLeaveTheCaptureOpen() {
        listOf(
            spec("  \$IF \$\$x \$THEN \$END\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;"),
            spec("  (a) NUMBER;\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;"),
            spec("  g NUMBER := &x;\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;")
        ).forEach {
            val e = rows(it)
            assertThat(e.status).describedAs(it).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
            assertThat(e.lookup.get().status).isEqualTo(Lookup.FOUND)
            assertThat(e.relationName).contains(n("HR", "EMPLOYEES"))
        }
    }

    @Test
    fun conservativeStopsAndSourceOnlyResults() {
        val s = prepare(
            "p.sql" to spec(
                "  SUBTYPE a IS b;\n  SUBTYPE b IS a;\n  TYPE cyc_t IS TABLE OF a;\n  TYPE deep_t IS TABLE OF a.b.c.d%TYPE;\n" +
                    "  TYPE num_t IS TABLE OF NUMBER;\n  TYPE rec_rows_t IS TABLE OF r_t;\n  TYPE unknown_t IS TABLE OF missing_t;"
            )
        )
        fun e(name: String) = s.describeExternalType(element(s, "p", name), n("p"), md, hr)
        assertThat(e("cyc_t").source.status).isEqualTo(Source.CYCLE)
        assertThat(e("cyc_t").status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(e("deep_t").status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(e("deep_t").anchorName).contains(n("a", "b", "c", "d"))
        assertThat(e("num_t").status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(e("num_t").anchor).isEmpty
        assertThat(e("rec_rows_t").source.status).isEqualTo(Source.DECLARED_TYPE)
        assertThat(e("rec_rows_t").status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(e("unknown_t").status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(s.describeExternalType(n("p", "r_t"), null, md, hr).status).isEqualTo(Status.NOT_EXTERNAL)
    }

    @Test
    fun incompleteIndexComposesTheElementAsSourceIncomplete() {
        val s = prepare("p_spec.sql" to pSpec, failing = true)
        assertThat(s.describeType(n("p", "rows_t"), null).status).isEqualTo(Source.UNRESOLVED_NAME)
        val e = s.describeExternalType(element(s, "p", "rows_t"), n("p"), md, hr)
        assertThat(e.status).isEqualTo(Status.SOURCE_INCOMPLETE)
        assertThat(fields(e)).containsExactly("\"ID\"", "\"SALARY\"", "\"NAME\"")
    }

    @Test
    fun subtypeAndValueChainsReachTheExternalAnchor() {
        val s = prepare(
            "p.sql" to "CREATE PACKAGE p AS SUBTYPE sal_t IS employees.salary%TYPE; v sal_t; g sal_t; END p;",
            "q.sql" to "CREATE PACKAGE q AS x p.g%TYPE; END q;"
        )
        val v = s.describeExternalType(valueRef(s, "p", "v"), n("p"), md, hr)
        assertThat(v.status).isEqualTo(Status.RESOLVED)
        assertThat(names(v.source.steps)).containsExactly("p.sal_t")
        assertThat(v.lookup.get().column.get().name.toString()).isEqualTo("\"SALARY\"")
        val x = s.describeExternalType(valueRef(s, "q", "x"), n("q"), md, hr)
        assertThat(x.status).isEqualTo(Status.RESOLVED)
        assertThat(names(x.source.steps)).containsExactly("p.g", "p.sal_t")
        assertThat(x.source.terminalReference.get().sourceRange.fileId).isEqualTo("p.sql")
    }

    // Decision order for source-resolved anchors.

    @Test
    fun followedAnchorsCompeteWithRelationReadings() {
        val s = prepare("p.sql" to "CREATE PACKAGE p AS g NUMBER; END p;", "q.sql" to "CREATE PACKAGE q AS x p.g%TYPE; END q;")
        val ref = valueRef(s, "q", "x")
        fun e(m: ProjectExternalMetadata, schema: ProjectIdentifier? = hr) = s.describeExternalType(ref, n("q"), m, schema)
        val relP = ProjectExternalRelation.of("HR", "P", true, listOf(ProjectExternalColumn.of("G", 1, null)))
        assertThat(e(ProjectExternalMetadata.none()).status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(e(md).status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(e(md).source.status).isEqualTo(Source.BUILT_IN)
        val conflict = e(metadata(employees, relP))
        assertThat(conflict.status).isEqualTo(Status.CONFLICT)
        assertThat(names(conflict.sourceCandidates)).containsExactly("p.g")
        assertThat(conflict.externalCandidates).containsExactly(relP)
        assertThat(conflict.anchorName).contains(n("p", "g"))
        assertThat(conflict.source.status).isEqualTo(Source.BUILT_IN)
        assertThat(e(metadata(employees, ProjectExternalRelation.of("HR", "P", true, listOf(idColumn)))).status).isEqualTo(Status.CONFLICT)
        assertThat(e(metadata(employees, ProjectExternalRelation.of("OTHER", "P", true, listOf(idColumn)))).status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(e(metadata(employees, relP), null).status).isEqualTo(Status.NOT_EXTERNAL)
    }

    @Test
    fun schemaQualifiedPackagesAndShorterPackageValuesCompeteToo() {
        val s = prepare(
            "a.sql" to "CREATE PACKAGE a.p AS g NUMBER; END p;",
            "r.sql" to "CREATE PACKAGE p AS TYPE r IS RECORD (f NUMBER); rec r; END p;",
            "q.sql" to "CREATE PACKAGE q AS x a.p.g%TYPE; y p.rec.f%TYPE; END q;"
        )
        val relAP = ProjectExternalRelation.of("A", "P", true, listOf(idColumn))
        val x = s.describeExternalType(valueRef(s, "q", "x"), n("q"), metadata(employees, relAP), hr)
        assertThat(x.source.status).isEqualTo(Source.BUILT_IN)
        assertThat(x.status).isEqualTo(Status.CONFLICT)
        assertThat(x.externalCandidates).containsExactly(relAP)
        assertThat(s.describeExternalType(valueRef(s, "q", "x"), n("q"), md, hr).status).isEqualTo(Status.NOT_EXTERNAL)
        val relP = ProjectExternalRelation.of("HR", "P", true, listOf(idColumn))
        val y = s.describeExternalType(valueRef(s, "q", "y"), n("q"), metadata(employees, relP), hr)
        assertThat(y.source.anchorStop).contains(ProjectTypeDescription.AnchorStop.FIELD_PATH)
        assertThat(y.status).isEqualTo(Status.CONFLICT)
        assertThat(names(y.sourceCandidates)).containsExactly("p.rec")
        assertThat(s.describeExternalType(valueRef(s, "q", "y"), n("q"), md, hr).status).isEqualTo(Status.NOT_EXTERNAL)
    }

    @Test
    fun schemaQualifiedWrittenAnchorsNeverResolve() {
        val qualified = w(TYPE, "hr", "employees", "salary")
        val appComplete = metadata(employees, complete = listOf("HR", "APP"))
        val capture = empty.describeExternalType(qualified, null, appComplete, app)
        assertThat(capture.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(capture.lookup.get().status).isEqualTo(Lookup.FOUND)
        assertThat(capture.relationName).contains(n("HR", "EMPLOYEES"))
        assertThat(appComplete.findColumn(hr, ProjectIdentifier.fromSource("EMPLOYEES"), ProjectIdentifier.fromSource("SALARY")).status).isEqualTo(Lookup.FOUND)
        assertThat(empty.describeExternalType(qualified, null, appComplete, null).status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        // Two positive readings: the current-schema relation HR and the qualified relation.
        val appHr = ProjectExternalRelation.of("APP", "HR", true, listOf(idColumn))
        val ambiguous = empty.describeExternalType(qualified, null, metadata(employees, appHr, complete = listOf("HR", "APP")), app)
        assertThat(ambiguous.status).isEqualTo(Status.AMBIGUOUS)
        assertThat(ambiguous.externalCandidates).containsExactly(appHr, employees)
        // A qualifier capture alone is not competition.
        val rowCapture = empty.describeExternalType(w(ROWTYPE, "hr", "employees"), null, metadata(appHr, complete = listOf("APP")), app)
        assertThat(rowCapture.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(rowCapture.externalCandidates).containsExactly(appHr)
        assertThat(rowCapture.lookup.get().status).isEqualTo(Lookup.NOT_COVERED)
        // The qualified relation without the column is not a positive reading either.
        val noColumn = empty.describeExternalType(w(TYPE, "hr", "employees", "bonus"), null, metadata(employees, appHr, complete = listOf("HR", "APP")), app)
        assertThat(noColumn.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(noColumn.lookup.get().status).isEqualTo(Lookup.COLUMN_NOT_FOUND)
        // Unqualified readings of the same names do resolve.
        assertThat(empty.describeExternalType(w(TYPE, "employees", "salary"), null, appComplete, hr).status).isEqualTo(Status.RESOLVED)
    }

    @Test
    fun siblingAndBodyScopes() {
        val s = prepare(
            "p.sql" to "CREATE PACKAGE p AS TYPE r IS RECORD (employees NUMBER, x employees%ROWTYPE, y employees.salary%TYPE); PROCEDURE run(a NUMBER); END p;",
            "b.sql" to "CREATE PACKAGE BODY p AS PROCEDURE run(r employees%ROWTYPE, s employees.salary%TYPE) IS BEGIN NULL; END; END p;"
        )
        val record = single(s, "p", "r") as ProjectPackageTypeView
        val x = s.describeExternalType(record.recordFields[1].typeRef, n("p"), md, hr)
        assertThat(x.status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(names(x.sourceCandidates)).containsExactly("p.r")
        assertThat(s.describeExternalType(record.recordFields[2].typeRef, n("p"), md, hr).status).isEqualTo(Status.NOT_EXTERNAL)
        val body = s.findDeclarations(n("p", "run")).declarations.single { it.fileId == "b.sql" } as ProjectPackageSubprogramView
        val row = s.describeExternalType(body.parameters[0].typeRef, n("p"), md, hr)
        assertThat(row.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(row.lookup.get().status).isEqualTo(Lookup.FOUND)
        // A body-private declaration may capture a column anchor too: not a definitive source answer.
        val column = s.describeExternalType(body.parameters[1].typeRef, n("p"), md, hr)
        assertThat(column.source.anchorStop).contains(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE)
        assertThat(column.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(column.lookup.get().status).isEqualTo(Lookup.FOUND)
    }

    @Test
    fun duplicateSpecificationsAreJudgedPerOccurrence() {
        val s = prepare(
            "a.sql" to spec("  CURSOR employees RETURN r_t;\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;"),
            "b.sql" to "CREATE PACKAGE p AS TYPE rows_t IS TABLE OF employees%ROWTYPE; END p;"
        )
        val elements = s.findDeclarations(n("p", "rows_t")).declarations.associate { it.fileId to (it as ProjectShapedTypeView).elementType.get() }
        assertThat(s.describeExternalType(elements.getValue("a.sql"), n("p"), md, hr).status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(s.describeExternalType(elements.getValue("b.sql"), n("p"), md, hr).status).isEqualTo(Status.RESOLVED)
        // A written anchor in package p cannot pick one specification.
        assertThat(s.describeExternalType(w(ROWTYPE, "employees"), n("p"), md, hr).status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
    }

    @Test
    fun writtenAnchorsUseTheWholeSpecificationOfTheirPackage() {
        val s = prepare("p.sql" to spec("  TYPE rows_t IS TABLE OF NUMBER;\n  CURSOR employees RETURN r_t;"), "q.sql" to "CREATE PACKAGE q AS g NUMBER; END q;")
        assertThat(s.describeExternalType(w(ROWTYPE, "employees"), n("p"), md, hr).status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(s.describeExternalType(w(ROWTYPE, "employees"), n("q"), md, hr).status).isEqualTo(Status.RESOLVED)
        assertThat(s.describeExternalType(w(ROWTYPE, "employees"), n("unknown_pkg"), md, hr).status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
    }

    // Conditional compilation: retained declarations of a $THEN branch may not exist for Oracle.

    private fun h(source: String, m: ProjectExternalMetadata = md): ProjectExternalTypeDescription {
        val s = prepare("p.sql" to source)
        return s.describeExternalType(valueRef(s, "p", "h"), n("p"), m, hr)
    }

    @Test
    fun conditionalCapturesAreNeverDefinitive() {
        val record = "  TYPE rec_t IS RECORD (salary VARCHAR2(30));\n"
        val fieldPath = h("CREATE PACKAGE p AS\n$record  \$IF \$\$use_local \$THEN\n    employees rec_t;\n  \$END\n  h employees.salary%TYPE;\nEND p;")
        assertThat(fieldPath.source.anchorStop).contains(ProjectTypeDescription.AnchorStop.FIELD_PATH)
        assertThat(fieldPath.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(fieldPath.lookup.get().status).isEqualTo(Lookup.FOUND)
        assertThat(fieldPath.relationName).contains(n("HR", "EMPLOYEES"))
        val unconditional = h("CREATE PACKAGE p AS\n$record  employees rec_t;\n  h employees.salary%TYPE;\nEND p;")
        assertThat(unconditional.status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(names(unconditional.sourceCandidates)).containsExactly("p.employees")

        val oneSegment = h("CREATE PACKAGE p AS \$IF \$\$f \$THEN employees NUMBER; \$END h employees%TYPE; END p;")
        assertThat(oneSegment.source.status).isEqualTo(Source.BUILT_IN)
        assertThat(oneSegment.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(names(oneSegment.sourceCandidates)).contains("p.employees")
        assertThat(h("CREATE PACKAGE p AS employees NUMBER; h employees%TYPE; END p;").status).isEqualTo(Status.NOT_EXTERNAL)

        val valueHop = h("CREATE PACKAGE p AS \$IF \$\$f \$THEN g NUMBER; \$ELSE g employees.salary%TYPE; \$END h g%TYPE; END p;")
        assertThat(valueHop.source.status).isEqualTo(Source.BUILT_IN)
        assertThat(valueHop.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(h("CREATE PACKAGE p AS g NUMBER; h g%TYPE; END p;").status).isEqualTo(Status.NOT_EXTERNAL)

        val chainHop = h("CREATE PACKAGE p AS\n$record  \$IF \$\$f \$THEN employees rec_t; \$END\n  a employees.salary%TYPE;\n  h a%TYPE;\nEND p;")
        assertThat(names(chainHop.source.steps)).containsExactly("p.a")
        assertThat(chainHop.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(chainHop.lookup.get().status).isEqualTo(Lookup.FOUND)
        assertThat(h("CREATE PACKAGE p AS\n$record  employees rec_t;\n  a employees.salary%TYPE;\n  h a%TYPE;\nEND p;").status).isEqualTo(Status.NOT_EXTERNAL)

        val subtypeHop = h("CREATE PACKAGE p AS \$IF \$\$f \$THEN SUBTYPE money_t IS NUMBER(10,2); \$ELSE SUBTYPE money_t IS employees.salary%TYPE; \$END h money_t; END p;")
        assertThat(names(subtypeHop.source.subtypes)).containsExactly("p.money_t")
        assertThat(subtypeHop.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(h("CREATE PACKAGE p AS SUBTYPE money_t IS NUMBER(10,2); h money_t; END p;").status).isEqualTo(Status.NOT_EXTERNAL)
    }

    @Test
    fun conditionalReferencesAndChainsAreNeverResolvedOrConflicting() {
        // A conditional subtype whose base is the external anchor.
        val subtype = h("CREATE PACKAGE p AS \$IF \$\$f \$THEN SUBTYPE sal_t IS employees.salary%TYPE; \$END h sal_t; END p;")
        assertThat(subtype.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(subtype.lookup.get().status).isEqualTo(Lookup.FOUND)
        assertThat(names(subtype.sourceCandidates)).contains("p.sal_t")
        assertThat(h("CREATE PACKAGE p AS SUBTYPE sal_t IS employees.salary%TYPE; h sal_t; END p;").status).isEqualTo(Status.RESOLVED)
        // The reference itself inside a branch.
        val reference = rows("CREATE PACKAGE p AS \$IF \$\$f \$THEN TYPE rows_t IS TABLE OF employees%ROWTYPE; \$END END p;")
        assertThat(reference.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(fields(reference)).containsExactly("\"ID\"", "\"SALARY\"", "\"NAME\"")
        // An unconditional member decides despite unrelated conditional compilation.
        val unrelated = rows(spec("  CURSOR employees RETURN r_t;\n  \$IF \$\$f \$THEN v NUMBER; \$END\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;"))
        assertThat(unrelated.status).isEqualTo(Status.NOT_EXTERNAL)
        // A conditional followed value and a positive relation reading: no CONFLICT, no NOT_EXTERNAL.
        val s = prepare("p.sql" to "CREATE PACKAGE p AS \$IF \$\$f \$THEN g NUMBER; \$END END p;", "q.sql" to "CREATE PACKAGE q AS x p.g%TYPE; END q;")
        val relP = ProjectExternalRelation.of("HR", "P", true, listOf(ProjectExternalColumn.of("G", 1, null)))
        val competing = s.describeExternalType(valueRef(s, "q", "x"), n("q"), metadata(employees, relP), hr)
        assertThat(competing.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(competing.externalCandidates).containsExactly(relP)
        assertThat(names(competing.sourceCandidates)).contains("p.g")
        assertThat(s.describeExternalType(valueRef(s, "q", "x"), n("q"), md, hr).status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
    }

    @Test
    fun conditionalHoldersNeverYieldDefinitiveCaptures() {
        // The reference sits in a branch; the cursor capturing it is unconditional.
        val packageCapture = "CREATE PACKAGE p AS\n  TYPE r_t IS RECORD (id NUMBER);\n  CURSOR employees RETURN r_t;\n" +
            "  \$IF \$\$use_rows \$THEN\n    TYPE rows_t IS TABLE OF employees%ROWTYPE;\n  \$END\nEND p;"
        val conditionalRows = rows(packageCapture)
        assertThat(conditionalRows.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(names(conditionalRows.sourceCandidates)).contains("p.rows_t")
        assertThat(rows(packageCapture.replace("  \$IF \$\$use_rows \$THEN\n", "").replace("  \$END\n", "")).status).isEqualTo(Status.NOT_EXTERNAL)

        // The whole record, sibling included, sits in a branch.
        val sibling = "CREATE PACKAGE p AS\n  \$IF \$\$use_record \$THEN\n    TYPE r_t IS RECORD (\n      employees NUMBER,\n" +
            "      row_value employees%ROWTYPE\n    );\n  \$END\nEND p;"
        fun rowValue(source: String): ProjectExternalTypeDescription {
            val s = prepare("p.sql" to source)
            return s.describeExternalType((single(s, "p", "r_t") as ProjectPackageTypeView).recordFields[1].typeRef, n("p"), md, hr)
        }
        val conditionalSibling = rowValue(sibling)
        assertThat(conditionalSibling.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(names(conditionalSibling.sourceCandidates)).contains("p.r_t")
        assertThat(rowValue(sibling.replace("  \$IF \$\$use_record \$THEN\n", "").replace("  \$END\n", "")).status).isEqualTo(Status.NOT_EXTERNAL)
        // An unrelated region does not make the unconditional record uncertain.
        assertThat(rowValue(sibling.replace("  \$IF \$\$use_record \$THEN\n", "  \$IF \$\$x \$THEN v NUMBER; \$END\n").replace("  \$END\nEND", "END")).status)
            .isEqualTo(Status.NOT_EXTERNAL)

        // A shorter package value stops a conditional anchor.
        val prefixStop = h("CREATE PACKAGE p AS TYPE r IS RECORD (f NUMBER); rec r; \$IF \$\$f \$THEN h p.rec.f%TYPE; \$END END p;")
        assertThat(prefixStop.source.anchorStop).contains(ProjectTypeDescription.AnchorStop.FIELD_PATH)
        assertThat(prefixStop.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(h("CREATE PACKAGE p AS TYPE r IS RECORD (f NUMBER); rec r; h p.rec.f%TYPE; END p;").status).isEqualTo(Status.NOT_EXTERNAL)

        // A later declaration rejects a conditional anchor (value order and member-name order).
        val later = h("CREATE PACKAGE p AS \$IF \$\$f \$THEN h p.g%TYPE; \$END g NUMBER; END p;")
        assertThat(later.source.anchorStop).contains(ProjectTypeDescription.AnchorStop.DECLARED_LATER)
        assertThat(later.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(h("CREATE PACKAGE p AS h p.g%TYPE; g NUMBER; END p;").status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(rows(spec("  \$IF \$\$f \$THEN TYPE rows_t IS TABLE OF employees%ROWTYPE; \$END\n  CURSOR employees RETURN r_t;")).status)
            .isEqualTo(Status.CAPTURE_NOT_EXCLUDED)

        // A conditional initial holder with a transitive value/subtype chain.
        val chain = h("CREATE PACKAGE p AS SUBTYPE sal_t IS NUMBER; g sal_t; \$IF \$\$f \$THEN h g%TYPE; \$END END p;")
        assertThat(names(chain.source.steps)).containsExactly("p.g", "p.sal_t")
        assertThat(chain.status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(names(chain.sourceCandidates)).contains("p.h")
        assertThat(h("CREATE PACKAGE p AS SUBTYPE sal_t IS NUMBER; g sal_t; h g%TYPE; END p;").status).isEqualTo(Status.NOT_EXTERNAL)

        // A source-only element in a branch.
        assertThat(rows("CREATE PACKAGE p AS \$IF \$\$f \$THEN TYPE rows_t IS TABLE OF NUMBER; \$END END p;").status).isEqualTo(Status.CAPTURE_NOT_EXCLUDED)
        assertThat(rows("CREATE PACKAGE p AS TYPE rows_t IS TABLE OF NUMBER; \$IF \$\$f \$THEN v NUMBER; \$END END p;").status).isEqualTo(Status.NOT_EXTERNAL)

        // A written anchor has no holder: its own lexical context is the caller's, package scope still decides.
        val s = prepare("p.sql" to spec("  CURSOR employees RETURN r_t;\n  \$IF \$\$f \$THEN v NUMBER; \$END"))
        assertThat(s.describeExternalType(w(ROWTYPE, "employees"), n("p"), md, hr).status).isEqualTo(Status.NOT_EXTERNAL)
    }

    @Test
    fun sourceDescriptionsAreUnchangedAndResultsImmutable() {
        val s = prepare("p_spec.sql" to pSpec)
        val ref = element(s, "p", "rows_t")
        val e = s.describeExternalType(ref, n("p"), md, hr)
        assertThat(e.source.toString()).isEqualTo(s.describeType(ref, n("p")).toString())
        assertThat(e.source.terminalReference.get().sourceRange).isEqualTo(s.describeType(ref, n("p")).terminalReference.get().sourceRange)
        assertThatThrownBy { (e.sourceCandidates as MutableList<ProjectDeclarationView>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (e.externalCandidates as MutableList<ProjectExternalRelation>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(Status.entries.map { it.name }).containsExactly(
            "RESOLVED", "NOT_EXTERNAL", "CAPTURE_NOT_EXCLUDED", "SOURCE_INCOMPLETE", "CONFLICT", "UNAVAILABLE",
            "SCHEMA_CONTEXT_MISSING", "AMBIGUOUS", "INCOMPLETE", "RELATION_NOT_FOUND", "COLUMN_NOT_FOUND", "REMOTE"
        )
        val pool = Executors.newFixedThreadPool(8)
        try {
            val results = (1..64).map { pool.submit<String> { s.describeExternalType(ref, n("p"), md, hr).toString() } }.map { it.get() }
            assertThat(results.toSet()).containsExactly(e.toString())
        } finally {
            pool.shutdown()
        }
    }
}
