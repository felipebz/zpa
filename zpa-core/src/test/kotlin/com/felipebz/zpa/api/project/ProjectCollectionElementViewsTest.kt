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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.reflect.ParameterizedType
import java.util.*

/** The direct collection element retained by the project facts, as seen through the public type views. */
@OptIn(ZpaExperimentalApi::class)
class ProjectCollectionElementViewsTest {
    private val pkg = """
        CREATE PACKAGE p AS
          TYPE rec_t IS RECORD (n NUMBER);
          TYPE a IS TABLE OF NUMBER;
          TYPE b IS TABLE OF rec_t INDEX BY PLS_INTEGER;
          TYPE c IS VARRAY(5) OF "Mixed";
          TYPE d IS ARRAY(3) OF q.type_name;
          TYPE e IS VARYING ARRAY(10) OF emp.sal%TYPE;
          TYPE f IS TABLE OF emp%ROWTYPE;
          TYPE g IS TABLE OF REF obj_t;
          TYPE h IS TABLE OF VARCHAR2(30 CHAR) NOT NULL;
          TYPE omitted IS TABLE OF t@link;
          TYPE cur IS REF CURSOR;
        END p;
    """.trimIndent()
    private val standalone = """
        CREATE TYPE s_tab AS TABLE OF NUMBER;
        /
        CREATE TYPE s_var AS VARRAY(5) OF app.rec_t;
        /
        CREATE TYPE s_arr AS VARYING ARRAY(5) OF (NUMBER NOT NULL) NOT PERSISTABLE;
        /
        CREATE TYPE s_obj AS OBJECT (a NUMBER);
        /
        CREATE TYPE s_force FORCE AS TABLE OF NUMBER;
        /
        CREATE TYPE s_w1 AS TABLE OF NUMBER
        /
        CREATE TYPE s_next AS TABLE OF DATE;
    """.trimIndent()

    private fun snapshot(): ProjectSnapshot = ProjectSnapshots.prepare(
        listOf(ProjectSourceInput.ofText("p.sql", pkg), ProjectSourceInput.ofText("s.sql", standalone)), false
    )

    private fun shaped(snapshot: ProjectSnapshot, vararg name: String) =
        snapshot.findDeclarations(ProjectQualifiedName.of(*name)).declarations.single() as ProjectShapedTypeView

    private fun internalElement(file: String, localName: String): TypeRef? {
        val text = if (file == "p.sql") pkg else standalone
        return ProjectDeclarationExtractor().extract(FileId(file), text).firstNotNullOf {
            when {
                it is PackageTypeDeclaration && it.name.originalSpelling == localName -> it to it.elementType
                it is StandaloneTypeDeclaration && it.name.last.originalSpelling == localName -> it to it.elementType
                else -> null
            }
        }.second
    }

    private fun assertSameAsFact(view: ProjectTypeReference, fact: TypeRef) {
        assertThat(view.name.name).isEqualTo(fact.name)
        assertThat(view.sourceRange).isEqualTo(fact.sourceRange.toPublic())
        when (fact) {
            is NamedTypeRef -> {
                assertThat(view.kind).isEqualTo(ProjectTypeReference.Kind.NAMED)
                assertThat(view.anchor).isEmpty
            }
            is AnchoredTypeRef -> {
                assertThat(view.kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
                assertThat(view.anchor.get().name).isEqualTo(fact.anchor.name)
            }
            is RefTypeRef -> {
                assertThat(view.kind).isEqualTo(ProjectTypeReference.Kind.REF)
                assertThat(view.anchor).isEmpty
            }
        }
    }

    @Test
    fun packageAndStandaloneCollectionsExposeTheirDirectElement() {
        val s = snapshot()
        val expected = listOf(
            Triple("p.sql", arrayOf("p", "a"), "NAMED NUMBER"), Triple("p.sql", arrayOf("p", "b"), "NAMED rec_t"),
            Triple("p.sql", arrayOf("p", "c"), "NAMED \"Mixed\""), Triple("p.sql", arrayOf("p", "d"), "NAMED q.type_name"),
            Triple("p.sql", arrayOf("p", "e"), "ANCHORED emp.sal"), Triple("p.sql", arrayOf("p", "f"), "ANCHORED emp"),
            Triple("p.sql", arrayOf("p", "g"), "REF obj_t"), Triple("p.sql", arrayOf("p", "h"), "NAMED VARCHAR2"),
            Triple("s.sql", arrayOf("s_tab"), "NAMED NUMBER"), Triple("s.sql", arrayOf("s_var"), "NAMED app.rec_t"),
            Triple("s.sql", arrayOf("s_arr"), "NAMED NUMBER")
        )
        expected.forEach { (file, name, text) ->
            val view = shaped(s, *name)
            assertThat(view.shape).describedAs(name.joinToString(".")).isEqualTo(Optional.of(ProjectTypeShape.COLLECTION))
            val element = view.elementType.get()
            assertThat("${element.kind.name.let { k -> if (k == "ANCHORED") "ANCHORED" else k }} ${element.name.segments.joinToString(".") { it.originalSpelling }}")
                .describedAs(name.joinToString(".")).isEqualTo(text)
            assertSameAsFact(element, internalElement(file, name.last())!!)
        }
        val e = shaped(s, "p", "e").elementType.get()
        assertThat(e.anchor).isEqualTo(Optional.of(ProjectTypeReference.Anchor.TYPE))
        assertThat(shaped(s, "p", "f").elementType.get().anchor).isEqualTo(Optional.of(ProjectTypeReference.Anchor.ROWTYPE))
        assertThat(shaped(s, "p", "c").elementType.get().name.last.isQuoted).isTrue()
        assertThat(shaped(s, "p", "h").elementType.get().sourceRange.endColumn - shaped(s, "p", "h").elementType.get().sourceRange.startColumn).isEqualTo("VARCHAR2(30 CHAR)".length)
    }

    @Test
    fun otherShapesAndConservativelyOmittedElementsHaveNoElement() {
        val s = snapshot()
        listOf(arrayOf("p", "rec_t"), arrayOf("p", "cur"), arrayOf("s_obj")).forEach {
            assertThat(shaped(s, *it).elementType).describedAs(it.joinToString(".")).isEmpty
        }
        assertThat(shaped(s, "s_obj").shape).isEqualTo(Optional.of(ProjectTypeShape.OBJECT))
        assertThat(shaped(s, "p", "cur").shape).isEqualTo(Optional.of(ProjectTypeShape.REF_CURSOR))
        // Collections whose element is unsupported (database link, FORCE header).
        listOf(arrayOf("p", "omitted"), arrayOf("s_force")).forEach {
            val view = shaped(s, *it)
            assertThat(view.shape).describedAs(it.joinToString(".")).isEqualTo(Optional.of(ProjectTypeShape.COLLECTION))
            assertThat(view.elementType).describedAs(it.joinToString(".")).isEmpty
        }
        // A collection ended by a `/` line without `;` keeps its element.
        assertThat(shaped(s, "s_w1").elementType.get().name).isEqualTo(ProjectQualifiedName.of("NUMBER"))
        assertThat(shaped(s, "s_next").elementType.get().name).isEqualTo(ProjectQualifiedName.of("DATE"))
    }

    @Test
    fun theElementIsAvailableThroughEveryExistingDeclarationQuery() {
        val s = snapshot()
        fun elementOf(view: ProjectDeclarationView) = (view as ProjectShapedTypeView).elementType.get()
        val viaAll = s.declarations.single { it.qualifiedName == ProjectQualifiedName.of("p", "b") }
        val viaFile = s.declarationsFor("p.sql").declarations.single { it.qualifiedName == ProjectQualifiedName.of("p", "b") }
        val viaName = s.findDeclarations(ProjectQualifiedName.of("p", "b")).declarations.single()
        val viaType = s.resolveNamedType(ProjectQualifiedName.of("b"), ProjectQualifiedName.of("p"))
        assertThat(viaType.status).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED)
        listOf(viaFile, viaName, viaType.declaration.get(), viaType.candidates.single()).forEach {
            assertThat(it.id).isEqualTo(viaAll.id)
            assertThat(elementOf(it).sourceRange).isEqualTo(elementOf(viaAll).sourceRange)
            assertThat(elementOf(it).name).isEqualTo(elementOf(viaAll).name)
        }
        val standaloneType = s.resolveNamedType(ProjectQualifiedName.of("s_var"))
        assertThat((standaloneType.declaration.get() as ProjectShapedTypeView).elementType.get().name).isEqualTo(ProjectQualifiedName.of("app", "rec_t"))
    }

    @Test
    fun theJavaAbiExposesOnlyPublicTypes() {
        val method = ProjectShapedTypeView::class.java.getMethod("getElementType")
        val returned = method.genericReturnType as ParameterizedType
        assertThat(returned.rawType).isEqualTo(Optional::class.java)
        assertThat(returned.actualTypeArguments.single()).isEqualTo(ProjectTypeReference::class.java)
        assertThat(ProjectShapedTypeView::class.java.methods.map { it.name }).contains("getShape", "getElementType")
        val signatures = ProjectShapedTypeView::class.java.methods.flatMap { listOf(it.genericReturnType.typeName) + it.genericParameterTypes.map { p -> p.typeName } }
        assertThat(signatures.filter { it.contains("com.felipebz") }).allMatch { it.contains("com.felipebz.zpa.api.project.") && !it.contains("zpa.project.TypeRef") }
        val s = snapshot()
        listOf(shaped(s, "p", "a"), shaped(s, "s_tab")).forEach { assertThat(it).isInstanceOf(ProjectShapedTypeView::class.java) }
        assertThat(ProjectDeclarationKind.entries.map { it.ordinal to it.name }.take(12)).containsExactly(
            0 to "PACKAGE", 1 to "PACKAGE_BODY", 2 to "STANDALONE_TYPE", 3 to "PACKAGE_TYPE", 4 to "PACKAGE_SUBTYPE", 5 to "PACKAGE_PROCEDURE",
            6 to "PACKAGE_FUNCTION", 7 to "SEQUENCE", 8 to "STANDALONE_PROCEDURE", 9 to "STANDALONE_FUNCTION", 10 to "PACKAGE_VARIABLE", 11 to "PACKAGE_CONSTANT"
        )
    }
}
