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
package com.felipebz.zpa.project

import com.felipebz.zpa.project.MemberNameStatus.*
import com.felipebz.zpa.project.PackageMemberKind.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/** Package specification member names and declaration positions, through the real lexer, extractor and index. */
class PackageMemberNamesTest {
    private val extractor = ProjectDeclarationExtractor()

    private fun prepare(vararg files: Pair<String, String>, concurrent: Boolean = false, failing: Boolean = false): ProjectIndexPreparationResult {
        val sources = files.map { (id, text) -> ProjectSource(FileId(id)) { text } } +
            (if (failing) listOf(ProjectSource(FileId("bad.sql")) { throw java.io.IOException("x") }) else emptyList())
        return ProjectIndexPreparation(extractor).prepare(sources, concurrent)
    }

    private fun id(s: String) = OracleIdentifier.fromSource(s)
    private fun name(vararg s: String) = QualifiedName(s.map(::id))

    private fun spec(index: ProjectSymbolIndex, owner: String, file: String? = null) = index.findOccurrences(name(owner)).single {
        val d = it.declaration
        d is PackageDeclaration && d.role == DeclarationRole.SPECIFICATION && (file == null || d.fileId.value == file)
    }

    private fun coverage(source: String) =
        extractor.extract(FileId("spec.sql"), source).filterIsInstance<PackageDeclaration>().single().memberNames!!

    private fun fact(index: ProjectSymbolIndex, vararg qualified: String) = index.findOccurrences(name(*qualified)).single().declaration

    /** The range of the retained element or value type reference of a member, i.e. a reference inside the spec. */
    private fun reference(index: ProjectSymbolIndex, vararg qualified: String): SourceRange = when (val d = fact(index, *qualified)) {
        is PackageTypeDeclaration -> (d.elementType ?: d.recordFields.first().typeRef).sourceRange
        is PackageValueDeclaration -> d.typeRef.sourceRange
        is PackageSubtypeDeclaration -> d.baseType.sourceRange
        else -> error("no reference in $d")
    }

    private fun status(source: String, member: String, referenceOwner: String): MemberNameStatus {
        val index = prepare("spec.sql" to source).index
        return PackageMemberNames.at(index, spec(index, "p"), id(member), reference(index, "p", referenceOwner)).status
    }

    @Test
    fun unretainedDeclarationsContributeNamesAndRetainedOnesAreNotRepeated() {
        val c = coverage(
            """
            CREATE PACKAGE p AS
              TYPE r_t IS RECORD (a NUMBER);
              CURSOR c1 RETURN r_t;
              CURSOR c2 (x NUMBER) IS SELECT 1 a FROM dual;
              e_bad EXCEPTION;
              PRAGMA EXCEPTION_INIT(e_bad, -20001);
              PRAGMA SERIALLY_REUSABLE;
              g NUMBER b;
              v NUMBER;
              SUBTYPE s IS NUMBER;
              PROCEDURE run;
              FUNCTION f RETURN NUMBER;
            END p;
            """.trimIndent()
        )
        // Cursors and exceptions are retained as facts; only the dropped value keeps a name.
        assertThat(c.unretained.map { it.kind to it.name.originalSpelling }).containsExactly(VALUE to "g")
        assertThat(c.complete).isTrue()
        assertThat(c.unretained[0].sourceRange).isEqualTo(SourceRange(FileId("spec.sql"), 8, 2, 8, 13))
    }

    @Test
    fun earlierCursorCapturesAndLaterCursorIsRejected() {
        val source = """
            CREATE PACKAGE p AS
              TYPE r_t IS RECORD (only_field NUMBER);
              CURSOR employees RETURN r_t;
              TYPE rows_t IS TABLE OF employees%ROWTYPE;
              TYPE late_t IS TABLE OF dept%ROWTYPE;
              CURSOR dept RETURN r_t;
              TYPE free_t IS TABLE OF jobs%ROWTYPE;
            END p;
        """.trimIndent()
        assertThat(status(source, "employees", "rows_t")).isEqualTo(DECLARED_BEFORE)
        assertThat(status(source, "dept", "late_t")).isEqualTo(REJECTED)
        assertThat(status(source, "jobs", "free_t")).isEqualTo(NOT_DECLARED)
    }

    @Test
    fun everyMemberKindIsOrderedAgainstTheReference() {
        val members = mapOf(
            "cursor" to "CURSOR employees RETURN r_t;",
            "variable" to "employees r_t;",
            "constant" to "employees CONSTANT NUMBER := 1;",
            "exception" to "employees EXCEPTION;",
            "record" to "TYPE employees IS RECORD (salary VARCHAR2(3));",
            "collection" to "TYPE employees IS TABLE OF NUMBER;",
            "subtype" to "SUBTYPE employees IS NUMBER;",
            "procedure" to "PROCEDURE employees;",
            "function" to "FUNCTION employees RETURN r_t;"
        )
        for ((kind, member) in members) {
            for ((ref, anchor) in listOf("rows_t" to "TYPE rows_t IS TABLE OF employees%ROWTYPE;", "h" to "h employees.salary%TYPE;")) {
                fun spec(first: String, second: String) = "CREATE PACKAGE p AS\n  TYPE r_t IS RECORD (only_field NUMBER);\n  $first\n  $second\nEND p;"
                assertThat(status(spec(member, anchor), "employees", ref)).describedAs("$kind before $ref").isEqualTo(DECLARED_BEFORE)
                assertThat(status(spec(anchor, member), "employees", ref)).describedAs("$kind after $ref").isEqualTo(REJECTED)
            }
        }
    }

    @Test
    fun aDeclarationContainingTheReferenceIsRejected() {
        assertThat(status("CREATE PACKAGE p AS employees employees%ROWTYPE; END p;", "employees", "employees")).isEqualTo(REJECTED)
        assertThat(status("CREATE PACKAGE p AS TYPE employees IS TABLE OF employees%ROWTYPE; END p;", "employees", "employees")).isEqualTo(REJECTED)
        assertThat(status("CREATE PACKAGE p AS TYPE x IS RECORD (employees NUMBER, f employees%ROWTYPE); END p;", "employees", "x"))
            .describedAs("record fields are not package members").isEqualTo(NOT_DECLARED)
    }

    @Test
    fun sameLinePositionsAreCompared() {
        val before = "CREATE PACKAGE p AS TYPE r_t IS RECORD (a NUMBER); CURSOR employees RETURN r_t; TYPE rows_t IS TABLE OF employees%ROWTYPE; END p;"
        val after = "CREATE PACKAGE p AS TYPE r_t IS RECORD (a NUMBER); TYPE rows_t IS TABLE OF employees%ROWTYPE; CURSOR employees RETURN r_t; END p;"
        assertThat(status(before, "employees", "rows_t")).isEqualTo(DECLARED_BEFORE)
        assertThat(status(after, "employees", "rows_t")).isEqualTo(REJECTED)
    }

    @Test
    fun identifierIdentityFollowsOracleRules() {
        val source = "CREATE PACKAGE p AS\n  CURSOR \"Emp\" RETURN r_t;\n  e_x EXCEPTION;\n  TYPE rows_t IS TABLE OF \"Emp\"%ROWTYPE;\nEND p;"
        val index = prepare("spec.sql" to source).index
        val ref = reference(index, "p", "rows_t")
        assertThat(PackageMemberNames.at(index, spec(index, "p"), id("\"Emp\""), ref).status).isEqualTo(DECLARED_BEFORE)
        assertThat(PackageMemberNames.at(index, spec(index, "p"), id("emp"), ref).status).isEqualTo(NOT_DECLARED)
        assertThat(PackageMemberNames.at(index, spec(index, "p"), id("\"E_X\""), ref).status).isEqualTo(DECLARED_BEFORE)
        assertThat(PackageMemberNames.at(index, spec(index, "p"), id("E_x"), ref).status).isEqualTo(DECLARED_BEFORE)
    }

    @Test
    fun repeatedNamesAreConservative() {
        val overloads = "CREATE PACKAGE p AS PROCEDURE employees; PROCEDURE employees(a NUMBER); TYPE rows_t IS TABLE OF employees%ROWTYPE; END p;"
        val index = prepare("spec.sql" to overloads).index
        val evidence = PackageMemberNames.at(index, spec(index, "p"), id("employees"), reference(index, "p", "rows_t"))
        assertThat(evidence.status).isEqualTo(DECLARED_BEFORE)
        assertThat(evidence.members).hasSize(2)
        val split = "CREATE PACKAGE p AS PROCEDURE employees; TYPE rows_t IS TABLE OF employees%ROWTYPE; employees EXCEPTION; END p;"
        assertThat(status(split, "employees", "rows_t")).isEqualTo(REJECTED)
    }

    @Test
    fun referencesOutsideTheSpecificationSeeEveryMember() {
        val source = "CREATE PACKAGE p AS TYPE rows_t IS TABLE OF employees%ROWTYPE; CURSOR employees RETURN rows_t; END p;"
        val index = prepare("spec.sql" to source).index
        assertThat(PackageMemberNames.at(index, spec(index, "p"), id("employees"), null).status).isEqualTo(DECLARED_BEFORE)
        assertThat(PackageMemberNames.at(index, spec(index, "p"), id("jobs"), null).status).isEqualTo(NOT_DECLARED)
    }

    @Test
    fun duplicateSpecificationsAndOtherFilesAreNeverMixed() {
        val a = "CREATE PACKAGE p AS CURSOR employees RETURN r_t; TYPE rows_t IS TABLE OF employees%ROWTYPE; END p;"
        val b = "CREATE PACKAGE p AS TYPE rows_t IS TABLE OF dept%ROWTYPE; END p;"
        val index = prepare("a.sql" to a, "b.sql" to b).index
        val specB = spec(index, "p", "b.sql")
        val refB = index.occurrencesFor(FileId("b.sql")).map { it.declaration }.filterIsInstance<PackageTypeDeclaration>().single().elementType!!.sourceRange
        // The cursor of a.sql is not a member of the b.sql occurrence.
        assertThat(PackageMemberNames.at(index, specB, id("employees"), refB).status).isEqualTo(NOT_DECLARED)
        assertThat(PackageMemberNames.at(index, spec(index, "p", "a.sql"), id("employees"), null).status).isEqualTo(DECLARED_BEFORE)
        // A reference from another file is not a position inside the specification.
        assertThatThrownBy { PackageMemberNames.at(index, spec(index, "p", "a.sql"), id("employees"), refB) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun conditionalCompilationAndSubstitutionLeaveNamesUnknown() {
        val cc = coverage("CREATE PACKAGE p AS CURSOR employees RETURN r_t; \$IF \$\$x \$THEN dept EXCEPTION; \$END END p;")
        assertThat(cc.gaps).containsExactly(MemberNameGap.CONDITIONAL_COMPILATION)
        val ccIndex = prepare("spec.sql" to "CREATE PACKAGE p AS CURSOR employees RETURN r_t; \$IF \$\$x \$THEN dept EXCEPTION; \$END END p;").index
        assertThat(PackageMemberNames.at(ccIndex, spec(ccIndex, "p"), id("employees"), null).status).isEqualTo(DECLARED_BEFORE)
        assertThat(PackageMemberNames.at(ccIndex, spec(ccIndex, "p"), id("dept"), null).status).isEqualTo(UNKNOWN)
        assertThat(PackageMemberNames.at(ccIndex, spec(ccIndex, "p"), id("jobs"), null).status).isEqualTo(UNKNOWN)
        assertThat(coverage("CREATE PACKAGE p AS g NUMBER := &x; END p;").gaps).contains(MemberNameGap.SUBSTITUTION)
    }

    private fun ccSpec(body: String) = "CREATE PACKAGE p AS\n  TYPE r_t IS RECORD (only_field NUMBER);\n$body\nEND p;"

    @Test
    fun membersOfAThenBranchAreNotProvenPresent() {
        // Oracle: the cursor captures only when the flag is TRUE; with FALSE the anchor reads the table.
        val conditional = ccSpec("  \$IF \$\$f \$THEN CURSOR employees RETURN r_t; \$END\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;")
        assertThat(status(conditional, "employees", "rows_t")).isEqualTo(UNKNOWN)
        assertThat(coverage(conditional).unretained).isEmpty()
        assertThat(prepare("spec.sql" to conditional).index.findOccurrences(name("p", "employees"))).hasSize(1)
        // Oracle: TRUE rejects the spec (PLS-00341), FALSE is valid with the table row.
        val later = ccSpec("  TYPE rows_t IS TABLE OF employees%ROWTYPE;\n  \$IF \$\$f \$THEN CURSOR employees RETURN r_t; \$END")
        assertThat(status(later, "employees", "rows_t")).isEqualTo(UNKNOWN)
        // A retained fact in a branch is kept as a fact, but is not proof either.
        val retained = ccSpec("  \$IF \$\$f \$THEN employees r_t; \$END\n  h employees.only_field%TYPE;")
        assertThat(status(retained, "employees", "h")).isEqualTo(UNKNOWN)
        assertThat(prepare("spec.sql" to retained).index.findOccurrences(name("p", "employees"))).hasSize(1)
    }

    @Test
    fun elseBranchesAndBranchesWithoutTheNameNeverProveAbsence() {
        // The lexer drops `$ELSE … $END`: Oracle captures with FALSE, nothing is known here.
        assertThat(status(ccSpec("  \$IF \$\$f \$THEN v NUMBER; \$ELSE CURSOR employees RETURN r_t; \$END\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;"), "employees", "rows_t"))
            .isEqualTo(UNKNOWN)
        assertThat(status(ccSpec("  \$IF \$\$f \$THEN v NUMBER; \$ELSE w NUMBER; \$END\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;"), "employees", "rows_t"))
            .isEqualTo(UNKNOWN)
        // Any later member rejects the reference, so a later conditional region also leaves the name open.
        assertThat(status(ccSpec("  TYPE rows_t IS TABLE OF employees%ROWTYPE;\n  \$IF \$\$f \$THEN v NUMBER; \$END"), "employees", "rows_t"))
            .isEqualTo(UNKNOWN)
        assertThat(status(ccSpec("  TYPE rows_t IS TABLE OF employees%ROWTYPE;"), "employees", "rows_t")).isEqualTo(NOT_DECLARED)
    }

    @Test
    fun unconditionalMembersStillDecide() {
        val before = ccSpec("  CURSOR employees RETURN r_t;\n  \$IF \$\$f \$THEN v NUMBER; \$END\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;")
        assertThat(status(before, "employees", "rows_t")).isEqualTo(DECLARED_BEFORE)
        val c = coverage(before)
        // The tokens `v NUMBER;` of the branch (columns are 0-based).
        assertThat(c.conditionalRange).isEqualTo(SourceRange(FileId("spec.sql"), 4, 16, 4, 25))
        val cursor = prepare("spec.sql" to before).index.findOccurrences(name("p", "employees")).single().declaration
        assertThat(c.isUnconditional(cursor.sourceRange)).isTrue()
        // An unconditional later member rejects whatever the branches hold.
        val rejected = ccSpec("  \$IF \$\$f \$THEN CURSOR employees RETURN r_t; \$END\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;\n  employees EXCEPTION;")
        assertThat(status(rejected, "employees", "rows_t")).isEqualTo(REJECTED)
        // An unconditional earlier member with a conditional later one: capture or rejection depends on the flag.
        val open = ccSpec("  CURSOR employees RETURN r_t;\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;\n  \$IF \$\$f \$THEN employees EXCEPTION; \$END")
        assertThat(status(open, "employees", "rows_t")).isEqualTo(UNKNOWN)
        // Self-contained directives bound no branch.
        val selfContained = ccSpec("  CURSOR employees RETURN r_t;\n  \$IF \$\$d \$THEN \$ERROR 'no' \$END \$END\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;")
        assertThat(coverage(selfContained).conditionalRange).isNull()
        assertThat(coverage(selfContained).gaps).containsExactly(MemberNameGap.CONDITIONAL_COMPILATION)
        assertThat(status(selfContained, "employees", "rows_t")).isEqualTo(DECLARED_BEFORE)
        // A quoted `$END` inside the error message does not close anything: the lexer reports one complete directive.
        val quotedEnd = ccSpec("  CURSOR employees RETURN r_t;\n  \$ERROR 'it''s \$END here' \$END\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;")
        assertThat(coverage(quotedEnd).conditionalRange).isNull()
        assertThat(status(quotedEnd, "employees", "rows_t")).isEqualTo(DECLARED_BEFORE)
    }

    @Test
    fun unboundedOrMixedDirectivesMakeEveryMemberConditional() {
        val lone = ccSpec("  CURSOR employees RETURN r_t;\n  \$END\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;")
        assertThat(status(lone, "employees", "rows_t")).isEqualTo(UNKNOWN)
        val mixed = ccSpec("  \$IF \$\$f \$THEN CURSOR employees RETURN r_t; &x \$END\n  TYPE rows_t IS TABLE OF employees%ROWTYPE;")
        assertThat(coverage(mixed).gaps).contains(MemberNameGap.SUBSTITUTION, MemberNameGap.CONDITIONAL_COMPILATION)
        assertThat(status(mixed, "employees", "rows_t")).isEqualTo(UNKNOWN)
        // Positions that cannot be matched with the text hide directives: no member is proven.
        val unverified = PackageMemberNameCoverage(emptyList(), listOf(MemberNameGap.UNVERIFIED_SOURCE_POSITIONS))
        assertThat(unverified.isUnconditional(SourceRange(FileId("f"), 1, 0, 1, 1))).isFalse()
    }

    @Test
    fun unidentifiedOrMalformedDeclarationsNeverInventNames() {
        val unidentified = coverage("CREATE PACKAGE p AS (a) NUMBER; FUNCTION f; v NUMBER; END p;")
        assertThat(unidentified.unretained).isEmpty()
        assertThat(unidentified.gaps).containsExactly(MemberNameGap.UNIDENTIFIED_DECLARATION)
        val unterminated = coverage("CREATE PACKAGE p AS PROCEDURE a(x NUMBER) PROCEDURE b; END p;")
        assertThat(unterminated.unretained.map { it.kind to it.name.originalSpelling }).containsExactly(PROCEDURE to "a")
        assertThat(unterminated.gaps).contains(MemberNameGap.UNTERMINATED_DECLARATION)
        val merged = coverage("CREATE PACKAGE p AS g NUMBER dept EXCEPTION; END p;")
        assertThat(merged.unretained.map { it.name.originalSpelling }).containsExactly("g")
        assertThat(merged.gaps).containsExactly(MemberNameGap.SWALLOWED_DECLARATION)
        // A variable named like a pragma keyword is a value, a pragma declares no name.
        assertThat(coverage("CREATE PACKAGE p AS pragma NUMBER b; PRAGMA SERIALLY_REUSABLE; END p;").unretained.map { it.kind })
            .containsExactly(VALUE)
    }

    @Test
    fun bodiesRecordNoNameCoverage() {
        val facts = extractor.extract(FileId("b.sql"), "CREATE PACKAGE BODY p AS CURSOR c IS SELECT 1 FROM dual; PROCEDURE r IS BEGIN NULL; END; END p;")
        assertThat(facts.filterIsInstance<PackageDeclaration>().map { it.memberNames }).allMatch { it == null }
    }

    @Test
    fun retainedMembersAreFoundThroughTheirIndexedOccurrences() {
        val source = """
            CREATE PACKAGE p AS
              g NUMBER;
              TYPE t IS TABLE OF NUMBER;
              SUBTYPE s IS NUMBER;
              PROCEDURE run;
              FUNCTION f RETURN NUMBER;
              CURSOR c RETURN t;
            END p;
        """.trimIndent()
        val index = prepare("spec.sql" to source).index
        val unit = spec(index, "p")
        val retained = index.occurrencesFor(FileId("spec.sql")).map { it.declaration }.filter { it !is PackageDeclaration }
        for (fact in retained) {
            val evidence = PackageMemberNames.at(index, unit, fact.qualifiedName().last, null)
            assertThat(evidence.members).describedAs(fact.toString()).containsExactly(fact.sourceRange)
        }
        // No unretained name duplicates a fact.
        val unretained = (unit.declaration as PackageDeclaration).memberNames!!.unretained
        assertThat(unretained).isEmpty()
        assertThat(unretained.map { it.sourceRange }).doesNotContainAnyElementsOf(retained.map { it.sourceRange })
    }

    @Test
    fun preparationFailuresAndOrderDoNotChangeCoverage() {
        val sources = (1..10).map { i ->
            "f$i.sql" to "CREATE PACKAGE p$i AS CURSOR c$i RETURN t; e$i EXCEPTION; ${if (i % 3 == 0) "\$IF \$\$x \$THEN \$END" else ""} g NUMBER; END p$i;"
        }
        fun coverages(result: ProjectIndexPreparationResult) = result.index.declarations.filterIsInstance<PackageDeclaration>()
            .associate { "${it.fileId.value}#${it.name}" to it.memberNames }
        val serial = coverages(prepare(*sources.toTypedArray()))
        assertThat(serial).hasSize(10)
        assertThat(coverages(prepare(*sources.toTypedArray(), concurrent = true))).isEqualTo(serial)
        assertThat(coverages(prepare(*sources.reversed().toTypedArray(), concurrent = true))).isEqualTo(serial)
        val failed = prepare(*sources.toTypedArray(), failing = true)
        assertThat(failed.failures).hasSize(1)
        assertThat(coverages(failed)).isEqualTo(serial)
        assertThat(serial.getValue("f3.sql#p3")!!.gaps).containsExactly(MemberNameGap.CONDITIONAL_COMPILATION)
        assertThat(serial.getValue("f1.sql#p1")!!.complete).isTrue()
    }

    @Test
    fun coverageIsImmutable() {
        val names = mutableListOf(PackageMemberName(id("c"), CURSOR, SourceRange(FileId("f"), 1, 0, 1, 5)))
        val gaps = mutableListOf(MemberNameGap.SUBSTITUTION)
        val c = PackageMemberNameCoverage(names, gaps)
        names.clear()
        gaps += MemberNameGap.UNIDENTIFIED_DECLARATION
        assertThat(c.unretained).hasSize(1)
        assertThat(c.gaps).containsExactly(MemberNameGap.SUBSTITUTION)
        assertThatThrownBy { (c.unretained as MutableList<PackageMemberName>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (c.gaps as MutableSet<MemberNameGap>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(c).isEqualTo(PackageMemberNameCoverage(listOf(PackageMemberName(id("C"), CURSOR, SourceRange(FileId("f"), 1, 0, 1, 5))), listOf(MemberNameGap.SUBSTITUTION)))
    }
}
