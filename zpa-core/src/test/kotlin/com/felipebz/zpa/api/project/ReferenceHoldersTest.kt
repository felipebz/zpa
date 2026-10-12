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
import com.felipebz.zpa.project.*
import com.felipebz.zpa.project.ProjectSourceReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The exact-name unit lookup must give the same occurrence as the original whole-file scan. */
@OptIn(ZpaExperimentalApi::class)
class ReferenceHoldersTest {
    private val files = listOf(
        "single.sql" to "CREATE PACKAGE a AS g NUMBER; h NUMBER; PROCEDURE p(x g%TYPE); END a;",
        "multi.sql" to "CREATE PACKAGE m1 AS g NUMBER; END m1;\n/\nCREATE PACKAGE m2 AS g NUMBER; k NUMBER; END m2;\n/\nCREATE PACKAGE app.\"Q\" AS v NUMBER; END;",
        "same.sql" to "CREATE PACKAGE d AS g NUMBER; END d;\n/\nCREATE PACKAGE d AS h NUMBER; END d;",
        "x1.sql" to "CREATE PACKAGE x AS g NUMBER; END x;",
        "x2.sql" to "CREATE PACKAGE x AS g DATE; END x;",
        "body.sql" to "CREATE PACKAGE sb AS g NUMBER; PROCEDURE w(a g%TYPE); END sb;\n/\nCREATE PACKAGE BODY sb AS PROCEDURE w(a g%TYPE) IS BEGIN NULL; END; END sb;",
        "loose.sql" to "CREATE PROCEDURE sp(a NUMBER) IS BEGIN NULL; END;\n/\nCREATE SEQUENCE seq;\n/\nCREATE TYPE t AS TABLE OF NUMBER;"
    )

    private fun index(concurrent: Boolean = false, list: List<Pair<String, String>> = files): ProjectSymbolIndex = ProjectIndexPreparation().prepare(
        list.map { (name, text) -> ProjectSource(FileId(name), ProjectSourceReader { text }) }, concurrent
    ).index

    private fun contains(outer: SourceRange, inner: SourceRange) =
        (outer.startLine < inner.startLine || (outer.startLine == inner.startLine && outer.startColumn <= inner.startColumn)) &&
            (inner.endLine < outer.endLine || (inner.endLine == outer.endLine && inner.endColumn <= outer.endColumn))

    /** The original implementation: every occurrence of the declaration's file. */
    private fun linearUnitOf(index: ProjectSymbolIndex, fact: ProjectDeclaration, owner: QualifiedName): IndexedDeclaration? =
        index.occurrencesFor(fact.fileId).filter {
            val unit = it.declaration
            unit is PackageDeclaration && unit.role == DeclarationRole.SPECIFICATION && unit.name == owner && contains(unit.sourceRange, fact.sourceRange)
        }.singleOrNull()

    private fun name(vararg s: String) = QualifiedName(s.map(OracleIdentifier::fromSource))

    private fun assertSame(index: ProjectSymbolIndex, fact: ProjectDeclaration, owner: QualifiedName) {
        val expected = linearUnitOf(index, fact, owner)
        val actual = ReferenceHolders.unitOf(index, fact, owner)
        if (expected == null) assertThat(actual).describedAs("$fact in $owner").isNull() else assertThat(actual).describedAs("$fact in $owner").isSameAs(expected)
    }

    @Test
    fun theOwnerLookupMatchesTheWholeFileScanForEveryFactAndOwner() {
        val index = index()
        val owners = index.declarations.filterIsInstance<PackageDeclaration>().map { it.name }.distinct() +
            name("absent") + name("app", "\"Q\"") + name("\"Q\"") + name("A")
        assertThat(index.declarations.count { it is PackageDeclaration && it.role == DeclarationRole.SPECIFICATION }).isGreaterThan(8)
        index.declarations.forEach { fact -> owners.forEach { assertSame(index, fact, it) } }
        // The serial, concurrent and reversed indexes give the same answers.
        listOf(index(true), index(false, files.reversed())).forEach { other ->
            other.declarations.forEach { fact -> owners.forEach { assertSame(other, fact, it) } }
        }
    }

    @Test
    fun expectedUnitsForRepresentativeFacts() {
        val index = index()
        fun spec(file: String, owner: QualifiedName) = index.occurrencesFor(FileId(file)).filter {
            val d = it.declaration; d is PackageDeclaration && d.role == DeclarationRole.SPECIFICATION && d.name == owner
        }
        val a = index.declarations.filterIsInstance<PackageValueDeclaration>().first { it.owner == name("a") }
        assertThat(ReferenceHolders.unitOf(index, a, name("a"))).isSameAs(spec("single.sql", name("a")).single())
        // Two units of one file are distinct; the same owner twice in one file is not a unique containing unit... except by range.
        val d = index.declarations.filterIsInstance<PackageValueDeclaration>().filter { it.owner == name("d") }
        assertThat(d.map { ReferenceHolders.unitOf(index, it, name("d")) }).containsExactlyElementsOf(spec("same.sql", name("d")))
        assertThat(ReferenceHolders.unitOf(index, d[0], name("m1"))).isNull()
        // The same package in two files: the unit is in the fact's own file.
        val x = index.declarations.filterIsInstance<PackageValueDeclaration>().filter { it.owner == name("x") }
        assertThat(x.map { it.fileId.value }).containsExactly("x1.sql", "x2.sql")
        x.forEach { assertThat(ReferenceHolders.unitOf(index, it, name("x"))!!.fileId).isEqualTo(it.fileId) }
        // Specification and body in one file: body facts have no containing specification.
        val body = index.declarations.first { it.role == DeclarationRole.BODY }
        assertThat(ReferenceHolders.unitOf(index, body, name("sb"))).isNull()
        // Qualified and quoted package names.
        val q = index.declarations.filterIsInstance<PackageValueDeclaration>().first { it.name.originalSpelling == "v" }
        assertThat(ReferenceHolders.unitOf(index, q, q.owner)).isNotNull
        assertThat(ReferenceHolders.unitOf(index, q, name("Q"))).isNull()
        // Facts without a package.
        index.occurrencesFor(FileId("loose.sql")).forEach { assertThat(ReferenceHolders.unitOf(index, it.declaration, name("sb"))).isNull() }
    }

    @Test
    fun exactRangeBoundariesAreInclusiveAndOutsideRangesAreRejected() {
        val index = index()
        val unit = index.occurrencesFor(FileId("single.sql")).single { it.declaration is PackageDeclaration }
        val spec = unit.declaration.sourceRange
        val owner = name("a")
        fun fact(range: SourceRange) = PackageValueDeclaration(
            owner, OracleIdentifier.fromSource("probe"), false, NamedTypeRef(name("NUMBER"), range), FileId("single.sql"), range
        )
        val inside = SourceRange(spec.fileId, spec.startLine, spec.startColumn + 1, spec.endLine, spec.endColumn - 1)
        val edgeStart = SourceRange(spec.fileId, spec.startLine, spec.startColumn, spec.startLine, spec.startColumn + 1)
        val edgeEnd = SourceRange(spec.fileId, spec.endLine, spec.endColumn - 1, spec.endLine, spec.endColumn)
        val beforeStart = SourceRange(spec.fileId, spec.startLine, 0, spec.startLine, 0).takeIf { spec.startColumn > 0 }
        val pastEnd = SourceRange(spec.fileId, spec.endLine, spec.endColumn, spec.endLine, spec.endColumn + 1)
        val earlierLine = SourceRange(spec.fileId, 1, 0, spec.startLine, spec.startColumn + 1).takeIf { spec.startLine > 1 }
        listOf(spec, inside, edgeStart, edgeEnd).forEach {
            assertThat(ReferenceHolders.unitOf(index, fact(it), owner)).isSameAs(unit)
            assertSame(index, fact(it), owner)
        }
        listOfNotNull(beforeStart, pastEnd, earlierLine, SourceRange(spec.fileId, spec.endLine + 1, 0, spec.endLine + 1, 4)).forEach {
            assertThat(ReferenceHolders.unitOf(index, fact(it), owner)).describedAs(it.toString()).isNull()
            assertSame(index, fact(it), owner)
        }
        // A fact of another file is never contained, even at an identical range.
        val other = PackageValueDeclaration(owner, OracleIdentifier.fromSource("probe"), false, NamedTypeRef(name("NUMBER"), spec), FileId("x1.sql"), spec)
        assertThat(ReferenceHolders.unitOf(index, other, owner)).isNull()
        assertSame(index, other, owner)
    }
}
