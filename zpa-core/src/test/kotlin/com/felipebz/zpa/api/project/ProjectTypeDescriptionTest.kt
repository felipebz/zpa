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

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.ProjectTypeDescription.Status
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*
import com.felipebz.zpa.api.project.ProjectBuiltInTypeCategory as Category
import com.felipebz.zpa.api.project.ProjectNamedTypeResolution.Status as Resolution

@OptIn(ZpaExperimentalApi::class)
class ProjectTypeDescriptionTest {
    private val spec = """
        CREATE PACKAGE p AS
          SUBTYPE money IS NUMBER;
          SUBTYPE amount IS money;
          SUBTYPE total IS amount;
          SUBTYPE rsub IS rec_t;
          SUBTYPE csub IS rows_t;
          SUBTYPE ext IS other.ext_t;
          SUBTYPE anch IS emp.sal%TYPE;
          SUBTYPE refd IS REF obj_t;
          SUBTYPE self IS self;
          SUBTYPE c1 IS c2;
          SUBTYPE c2 IS c3;
          SUBTYPE c3 IS c1;
          SUBTYPE "Q" IS NUMBER;
          SUBTYPE qq IS "Q";
          SUBTYPE q2 IS q;
          SUBTYPE dbl IS SYS.NUMBER;
          TYPE rec_t IS RECORD (n total, items rows_t);
          TYPE rows_t IS TABLE OF rec_t;
          vn NUMBER;
          vc VARCHAR2(10);
          vl CLOB;
          vb BOOLEAN;
          vd DATE;
          vj JSON;
          anch_rec rec_t%TYPE;
        END p;
    """.trimIndent()
    private val standalone = """
        CREATE TYPE ids AS TABLE OF NUMBER;
        /
        CREATE TYPE s_var AS VARRAY(5) OF app.rec_t;
    """.trimIndent()

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)
    private val p = n("p")
    private fun prepare(concurrent: Boolean = false, vararg extra: ProjectSourceInput, files: List<Pair<String, String>> = listOf("p.sql" to spec, "s.sql" to standalone)) =
        ProjectSnapshots.prepare(files.map { (id, text) -> ProjectSourceInput.ofText(id, text) } + extra, concurrent)

    private val snapshot = prepare()

    private fun view(vararg name: String) = snapshot.findDeclarations(n(*name)).declarations.single()
    private fun ids(description: ProjectTypeDescription) = description.subtypes.map { it.id }
    private fun aliases(vararg names: String) = names.map { view("p", it).id }
    private fun value(name: String) = view("p", name) as ProjectPackageValueView

    @Test
    fun builtInNamesAndReferencesCoverEverySupportedCategory() {
        listOf("NUMBER" to Category.NUMERIC, "VARCHAR2" to Category.CHARACTER, "CLOB" to Category.LOB, "BOOLEAN" to Category.BOOLEAN,
            "DATE" to Category.DATE, "JSON" to Category.JSON).forEach { (name, category) ->
            val byName = snapshot.describeType(n(name), p)
            assertThat(byName.status).describedAs(name).isEqualTo(Status.BUILT_IN)
            assertThat(byName.builtInCategory).isEqualTo(Optional.of(category))
            assertThat(byName.subtypes).isEmpty()
            assertThat(byName.declaration).isEmpty
            assertThat(byName.resolution).isEmpty
            // A name has no source range: none is invented.
            assertThat(byName.terminalReference).isEmpty
        }
        listOf("vn" to Category.NUMERIC, "vc" to Category.CHARACTER, "vl" to Category.LOB, "vb" to Category.BOOLEAN, "vd" to Category.DATE, "vj" to Category.JSON).forEach { (v, category) ->
            val reference = value(v).typeRef
            val described = snapshot.describeType(reference, p)
            assertThat(described.builtInCategory).describedAs(v).isEqualTo(Optional.of(category))
            assertThat(described.terminalReference.get().sourceRange).isEqualTo(reference.sourceRange)
        }
    }

    @Test
    fun builtInClassificationKeepsTheExistingPrecedence() {
        // Qualified and quoted names are not built-ins; they remain project names.
        assertThat(snapshot.describeType(n("SYS", "NUMBER"), p).status).isEqualTo(Status.UNRESOLVED_NAME)
        assertThat(snapshot.describeType(n("\"NUMBER\""), p).resolution.get().status).isEqualTo(Resolution.NOT_FOUND_IN_PROJECT)
        assertThat(snapshot.describeType(n("unknown_t"), p).builtInCategory).isEmpty
        // A recognized built-in needs no prepared index.
        val unprepared = unpreparedSnapshot()
        assertThat(unprepared.describeType(n("NUMBER"), null).builtInCategory).isEqualTo(Optional.of(Category.NUMERIC))
    }

    @Test
    fun subtypeAliasesAreFollowedInOrderWithTheDeclaringOwner() {
        val money = snapshot.describeType(n("money"), p)
        assertThat(money.status).isEqualTo(Status.BUILT_IN)
        assertThat(ids(money)).isEqualTo(aliases("money"))
        val total = snapshot.describeType(n("total"), p)
        assertThat(total.status).isEqualTo(Status.BUILT_IN)
        assertThat(total.builtInCategory).isEqualTo(Optional.of(Category.NUMERIC))
        assertThat(ids(total)).isEqualTo(aliases("total", "amount", "money"))
        assertThat(total.subtypes.map { it.qualifiedName }).containsExactly(n("p", "total"), n("p", "amount"), n("p", "money"))
        // The terminal reference is the real retained base reference of the last alias.
        val last = total.subtypes.last().baseType
        assertThat(total.terminalReference.get().sourceRange).isEqualTo(last.sourceRange)
        assertThat(total.terminalReference.get().name).isEqualTo(n("NUMBER"))
        // A qualified name uses its own prefix: no package context needed.
        assertThat(ids(snapshot.describeType(n("p", "total"), null))).isEqualTo(aliases("total", "amount", "money"))
        // A one-segment name without context does not fall back to the package.
        assertThat(snapshot.describeType(n("total"), null).resolution.get().status).isEqualTo(Resolution.NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun descriptionsStopAtTheFirstDeclaredTypeWithoutTraversingItsMembers() {
        val record = snapshot.describeType(n("rec_t"), p)
        assertThat(record.status).isEqualTo(Status.DECLARED_TYPE)
        assertThat((record.declaration.get() as ProjectShapedTypeView).shape).isEqualTo(Optional.of(ProjectTypeShape.RECORD))
        assertThat(record.declaration.get().id).isEqualTo(view("p", "rec_t").id)
        assertThat(record.subtypes).isEmpty()
        assertThat(record.terminalReference).isEmpty

        val viaSubtype = snapshot.describeType(n("rsub"), p)
        assertThat(viaSubtype.status).isEqualTo(Status.DECLARED_TYPE)
        assertThat(viaSubtype.declaration.get().id).isEqualTo(view("p", "rec_t").id)
        assertThat(ids(viaSubtype)).isEqualTo(aliases("rsub"))
        val viaCollection = snapshot.describeType(n("csub"), p)
        assertThat((viaCollection.declaration.get() as ProjectShapedTypeView).shape).isEqualTo(Optional.of(ProjectTypeShape.COLLECTION))

        // Field using a subtype chain; collection element referencing a record; record field referencing a collection.
        val record2 = view("p", "rec_t") as ProjectPackageTypeView
        val field = snapshot.describeType(record2.recordFields[0].typeRef, p)
        assertThat(field.status).isEqualTo(Status.BUILT_IN)
        assertThat(ids(field)).isEqualTo(aliases("total", "amount", "money"))
        val element = snapshot.describeType((view("p", "rows_t") as ProjectShapedTypeView).elementType.get(), p)
        assertThat(element.status).isEqualTo(Status.DECLARED_TYPE)
        assertThat(element.declaration.get().id).isEqualTo(view("p", "rec_t").id)
        val items = snapshot.describeType(record2.recordFields[1].typeRef, p)
        assertThat(items.declaration.get().id).isEqualTo(view("p", "rows_t").id)
        assertThat(items.terminalReference.get().sourceRange).isEqualTo(record2.recordFields[1].typeRef.sourceRange)
    }

    @Test
    fun standaloneTypesUseTheStandaloneNamespace() {
        val ids = snapshot.describeType(n("ids"), null)
        assertThat(ids.status).isEqualTo(Status.DECLARED_TYPE)
        assertThat(ids.declaration.get().kind).isEqualTo(ProjectDeclarationKind.STANDALONE_TYPE)
        assertThat(snapshot.describeType((view("ids") as ProjectShapedTypeView).elementType.get(), null).builtInCategory).isEqualTo(Optional.of(Category.NUMERIC))
        val external = snapshot.describeType((view("s_var") as ProjectShapedTypeView).elementType.get(), null)
        assertThat(external.status).isEqualTo(Status.UNRESOLVED_NAME)
        assertThat(external.resolution.get().status).isEqualTo(Resolution.NOT_FOUND_IN_PROJECT)
        // With a package context a one-segment standalone name is not found.
        assertThat(snapshot.describeType(n("ids"), p).resolution.get().status).isEqualTo(Resolution.NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun quotedNamesKeepTheirDistinction() {
        val quoted = snapshot.describeType(n("qq"), p)
        assertThat(quoted.status).isEqualTo(Status.BUILT_IN)
        assertThat(ids(quoted)).isEqualTo(aliases("qq", "\"Q\""))
        val unquotedBase = snapshot.describeType(n("q2"), p)
        assertThat(unquotedBase.status).isEqualTo(Status.UNRESOLVED_NAME)
        assertThat(ids(unquotedBase)).isEqualTo(aliases("q2"))
        assertThat(unquotedBase.terminalReference.get().name).isEqualTo(n("q"))
    }

    @Test
    fun externalAndQualifiedBuiltInLookalikesRemainUnresolvedNames() {
        val external = snapshot.describeType(n("ext"), p)
        assertThat(external.status).isEqualTo(Status.UNRESOLVED_NAME)
        assertThat(external.resolution.get().status).isEqualTo(Resolution.NOT_FOUND_IN_PROJECT)
        assertThat(external.resolution.get().name).isEqualTo(n("other", "ext_t"))
        assertThat(ids(external)).isEqualTo(aliases("ext"))
        assertThat(external.terminalReference.get().sourceRange).isEqualTo((view("p", "ext") as ProjectSubtypeView).baseType.sourceRange)
        assertThat(snapshot.describeType(n("dbl"), p).status).isEqualTo(Status.UNRESOLVED_NAME)
    }

    @Test
    fun anchoredAndRefReferencesStopWithoutResolution() {
        // emp.sal%TYPE names no supplied value: the source-backed lookup is reported, not an anchored stop.
        val anchoredViaSubtype = snapshot.describeType(n("anch"), p)
        assertThat(anchoredViaSubtype.status).isEqualTo(Status.UNRESOLVED_VALUE)
        assertThat(anchoredViaSubtype.valueResolution.get().status).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT)
        assertThat(ids(anchoredViaSubtype)).isEqualTo(aliases("anch"))
        assertThat(anchoredViaSubtype.terminalReference.get().anchor).isEqualTo(Optional.of(ProjectTypeReference.Anchor.TYPE))
        val ref = snapshot.describeType(n("refd"), p)
        assertThat(ref.status).isEqualTo(Status.REF_REFERENCE)
        assertThat(ref.terminalReference.get().kind).isEqualTo(ProjectTypeReference.Kind.REF)
        // The supplied reference overload stops at once, even when its name matches a project type.
        val matching = value("anch_rec").typeRef
        assertThat(matching.kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
        val described = snapshot.describeType(matching, p)
        assertThat(described.status).isEqualTo(Status.ANCHORED_REFERENCE)
        // rec_t is a type of the package, which captures the one-segment name before any value is considered.
        assertThat(described.anchorStop).isEqualTo(Optional.of(ProjectTypeDescription.AnchorStop.ENCLOSING_SCOPE))
        assertThat(described.subtypes).isEmpty()
        assertThat(described.declaration).isEmpty
        assertThat(described.terminalReference.get().sourceRange).isEqualTo(matching.sourceRange)
    }

    @Test
    fun cyclesAreReportedOnceByOccurrence() {
        val self = snapshot.describeType(n("self"), p)
        assertThat(self.status).isEqualTo(Status.CYCLE)
        assertThat(ids(self)).isEqualTo(aliases("self"))
        assertThat(self.terminalReference.get().sourceRange).isEqualTo((view("p", "self") as ProjectSubtypeView).baseType.sourceRange)
        val three = snapshot.describeType(n("c1"), p)
        assertThat(three.status).isEqualTo(Status.CYCLE)
        assertThat(ids(three)).isEqualTo(aliases("c1", "c2", "c3"))
        assertThat(ids(snapshot.describeType(n("c2"), p))).isEqualTo(aliases("c2", "c3", "c1"))
        val two = prepare(files = listOf("x.sql" to "CREATE PACKAGE x AS SUBTYPE a IS b; SUBTYPE b IS a; END x;"))
        val twoCycle = two.describeType(n("a"), n("x"))
        assertThat(twoCycle.status).isEqualTo(Status.CYCLE)
        assertThat(twoCycle.subtypes.map { it.localName.originalSpelling }).containsExactly("a", "b")
    }

    @Test
    fun longAcyclicChainsAreNotLimitedNorRecursive() {
        val size = 5000
        val source = "CREATE PACKAGE big AS\n  SUBTYPE s0 IS DATE;\n" + (1 until size).joinToString("\n") { "  SUBTYPE s$it IS s${it - 1};" } + "\nEND big;"
        val big = prepare(files = listOf("big.sql" to source))
        val described = big.describeType(n("s${size - 1}"), n("big"))
        assertThat(described.status).isEqualTo(Status.BUILT_IN)
        assertThat(described.builtInCategory).isEqualTo(Optional.of(Category.DATE))
        assertThat(described.subtypes).hasSize(size)
        assertThat(described.subtypes.map { it.id }.toSet()).hasSize(size)
    }

    @Test
    fun duplicateDeclarationsStopBeforeSelectingACandidate() {
        val files = listOf("a.sql" to "CREATE PACKAGE q AS SUBTYPE amount IS m; SUBTYPE m IS NUMBER; SUBTYPE only_a IS NUMBER; END q;",
            "b.sql" to "CREATE PACKAGE q AS SUBTYPE m IS DATE; END q;")
        val s = prepare(files = files)
        val duplicate = s.describeType(n("m"), n("q"))
        assertThat(duplicate.status).isEqualTo(Status.UNRESOLVED_NAME)
        assertThat(duplicate.resolution.get().status).isEqualTo(Resolution.AMBIGUOUS)
        assertThat(duplicate.resolution.get().candidates).hasSize(2)
        assertThat(duplicate.subtypes).isEmpty()
        val chained = s.describeType(n("amount"), n("q"))
        assertThat(chained.status).isEqualTo(Status.UNRESOLVED_NAME)
        assertThat(chained.resolution.get().status).isEqualTo(Resolution.AMBIGUOUS)
        assertThat(chained.subtypes.map { it.localName.originalSpelling }).containsExactly("amount")
        assertThat(s.describeType(n("only_a"), n("q")).status).isEqualTo(Status.BUILT_IN)
    }

    @Test
    fun preparationFailuresTakePrecedenceAndUnpreparedSnapshotsAreReported() {
        val failed = prepare(false, ProjectSourceInput.ofReader("bad.sql") { throw java.io.IOException("x") })
        assertThat(failed.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_FAILURES)
        val unique = failed.describeType(n("money"), p)
        assertThat(unique.status).isEqualTo(Status.UNRESOLVED_NAME)
        assertThat(unique.resolution.get().status).isEqualTo(Resolution.INCOMPLETE_INDEX)
        assertThat(unique.resolution.get().failures).hasSize(1)
        assertThat(unique.resolution.get().candidates).hasSize(1)
        assertThat(failed.describeType(n("NUMBER"), p).status).isEqualTo(Status.BUILT_IN)
        val unprepared = unpreparedSnapshot().describeType(n("money"), p)
        assertThat(unprepared.status).isEqualTo(Status.UNRESOLVED_NAME)
        assertThat(unprepared.resolution.get().status).isEqualTo(Resolution.NOT_PREPARED)
    }

    @Test
    fun resultsAreIdenticalForSerialConcurrentAndReversedPreparation() {
        fun summary(s: ProjectSnapshot) = listOf(n("total"), n("rsub"), n("c1"), n("ext"), n("anch"), n("rec_t")).map {
            val d = s.describeType(it, p)
            listOf(d.status, d.subtypes.map { v -> v.qualifiedName to v.id.ordinal }, d.declaration.map { v -> v.qualifiedName }, d.builtInCategory)
        }
        val files = listOf("p.sql" to spec, "s.sql" to standalone)
        val serial = summary(prepare(false, files = files))
        assertThat(summary(prepare(true, files = files))).isEqualTo(serial)
        assertThat(summary(prepare(false, files = files.reversed()))).isEqualTo(serial)
    }

    @Test
    fun javaAbiAndImmutability() {
        val byName = ProjectSnapshot::class.java.getMethod("describeType", ProjectQualifiedName::class.java, ProjectQualifiedName::class.java)
        val byReference = ProjectSnapshot::class.java.getMethod("describeType", ProjectTypeReference::class.java, ProjectQualifiedName::class.java)
        assertThat(byName.returnType).isEqualTo(ProjectTypeDescription::class.java)
        assertThat(byReference.returnType).isEqualTo(ProjectTypeDescription::class.java)
        val getters = ProjectTypeDescription::class.java.declaredMethods.filter { it.name.startsWith("get") && java.lang.reflect.Modifier.isPublic(it.modifiers) }
            .associate { it.name to it.genericReturnType.typeName }
        assertThat(getters).containsEntry("getStatus", ProjectTypeDescription.Status::class.java.name)
        assertThat(getters["getSubtypes"]).isEqualTo("java.util.List<com.felipebz.zpa.api.project.ProjectSubtypeView>")
        assertThat(getters["getDeclaration"]).isEqualTo("java.util.Optional<com.felipebz.zpa.api.project.ProjectTypeDeclarationView>")
        assertThat(getters["getBuiltInCategory"]).isEqualTo("java.util.Optional<com.felipebz.zpa.api.project.ProjectBuiltInTypeCategory>")
        assertThat(getters["getTerminalReference"]).isEqualTo("java.util.Optional<com.felipebz.zpa.api.project.ProjectTypeReference>")
        assertThat(getters["getResolution"]).isEqualTo("java.util.Optional<com.felipebz.zpa.api.project.ProjectNamedTypeResolution>")
        assertThat(getters["getSteps"]).isEqualTo("java.util.List<com.felipebz.zpa.api.project.ProjectDeclarationView>")
        assertThat(getters["getValues"]).isEqualTo("java.util.List<com.felipebz.zpa.api.project.ProjectPackageValueView>")
        assertThat(getters["getValueResolution"]).isEqualTo("java.util.Optional<com.felipebz.zpa.api.project.ProjectValueResolution>")
        assertThat(getters["getAnchorStop"]).isEqualTo("java.util.Optional<com.felipebz.zpa.api.project.ProjectTypeDescription\$AnchorStop>")
        assertThat((ProjectTypeDescription::class.java.getMethod("getSteps").genericReturnType as ParameterizedType).actualTypeArguments.single()).isEqualTo(ProjectDeclarationView::class.java)
        assertThat((ProjectTypeDescription::class.java.getMethod("getValues").genericReturnType as ParameterizedType).actualTypeArguments.single()).isEqualTo(ProjectPackageValueView::class.java)
        assertThat(ProjectTypeDescription::class.java.declaredMethods.filter { it.name.startsWith("get") && Modifier.isPublic(it.modifiers) }.map { it.name })
            .containsExactlyInAnyOrder("getStatus", "getSteps", "getValues", "getSubtypes", "getDeclaration", "getBuiltInCategory", "getTerminalReference", "getResolution", "getValueResolution", "getAnchorStop")
        assertThat((ProjectTypeDescription::class.java.getMethod("getSubtypes").genericReturnType as ParameterizedType).actualTypeArguments.single()).isEqualTo(ProjectSubtypeView::class.java)
        assertThat(ProjectTypeDescription.Status.entries.map { it.ordinal to it.name }).containsExactly(
            0 to "DECLARED_TYPE", 1 to "BUILT_IN", 2 to "UNRESOLVED_NAME", 3 to "ANCHORED_REFERENCE", 4 to "REF_REFERENCE", 5 to "CYCLE",
            6 to "UNRESOLVED_VALUE"
        )
        assertThat(ProjectTypeDescription.AnchorStop.entries.map { it.name }).containsExactly("ROWTYPE", "FIELD_PATH", "ENCLOSING_SCOPE", "DECLARED_LATER")
        assertThat(Category.entries.map { it.name }).containsExactly("NUMERIC", "CHARACTER", "LOB", "BOOLEAN", "DATE", "JSON")
        val described = snapshot.describeType(n("total"), p)
        assertThatThrownBy { (described.subtypes as MutableList<ProjectSubtypeView>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (described.subtypes as MutableList<ProjectSubtypeView>).add(described.subtypes[0]) }.isInstanceOf(UnsupportedOperationException::class.java)
    }

    private fun unpreparedSnapshot(): ProjectSnapshot {
        val probe = object : PlSqlCheck() {
            var captured: ProjectSnapshot? = null
            init { subscribeTo(PlSqlGrammar.PROCEDURE_DECLARATION) }
            override fun visitNode(node: AstNode) { captured = projectAnalysis().snapshot }
        }
        val file = object : PlSqlFile {
            override fun contents() = "CREATE PACKAGE BODY b AS PROCEDURE w IS BEGIN NULL; END w; END b;"
            override fun fileName() = "b.sql"
            override fun path(): Path = Path.of("b.sql")
            override fun type() = PlSqlFile.Type.MAIN
        }
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, ProjectAnalysisContext.NOT_PREPARED).scanFile(file, listOf(probe), FileId("b.sql"))
        return probe.captured!!
    }
}
