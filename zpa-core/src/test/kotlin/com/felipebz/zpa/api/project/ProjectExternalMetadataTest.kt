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
import com.felipebz.zpa.api.project.ProjectExternalDatatype.LengthSemantics.BYTE
import com.felipebz.zpa.api.project.ProjectExternalDatatype.LengthSemantics.CHAR
import com.felipebz.zpa.api.project.ProjectExternalLookup.Status
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import com.felipebz.zpa.api.project.ProjectBuiltInTypeCategory as Category

@OptIn(ZpaExperimentalApi::class)
class ProjectExternalMetadataTest {
    private fun id(spelling: String) = ProjectIdentifier.fromSource(spelling)
    private fun type(name: String, owner: String? = null, length: Int? = null, semantics: ProjectExternalDatatype.LengthSemantics? = null, precision: Int? = null, scale: Int? = null) =
        ProjectExternalDatatype.of(name, owner, length, semantics, precision, scale)
    private fun col(name: String, position: Int?, datatype: ProjectExternalDatatype? = type("NUMBER")) = ProjectExternalColumn.of(name, position, datatype)

    private val idCol = col("ID", 1, type("NUMBER", precision = 8, scale = 0))
    private val salaryCol = col("SALARY", 2, type("NUMBER", precision = 10, scale = 2))
    private val nameCol = col("NAME", 3, type("VARCHAR2", length = 30, semantics = BYTE))
    private val employees = ProjectExternalRelation.of("HR", "EMPLOYEES", true, listOf(nameCol, idCol, salaryCol))
    private val hr = ProjectExternalMetadata.builder("fixture").completeSchema("HR").relation(employees).build()

    @Test
    fun employeesFixtureLooksUpColumnsAndOrdersRowFieldsByColumnId() {
        assertThat(employees.rowFields.map { it.name.originalSpelling }).containsExactly("\"ID\"", "\"SALARY\"", "\"NAME\"")
        assertThat(employees.columns).containsExactly(idCol, salaryCol, nameCol)
        assertThat(employees.qualifiedName).isEqualTo(ProjectQualifiedName.of("hr", "employees"))
        val salary = hr.findColumn(id("hr"), id("employees"), id("salary"))
        assertThat(salary.status).isEqualTo(Status.FOUND)
        assertThat(salary.column.get()).isSameAs(salaryCol)
        assertThat(salary.relation.get()).isSameAs(employees)
        assertThat(salary.origin).contains("fixture")
        val datatype = salary.column.get().datatype.get()
        assertThat(datatype.name).isEqualTo("NUMBER")
        assertThat(datatype.precision).isEqualTo(OptionalInt.of(10))
        assertThat(datatype.scale).isEqualTo(OptionalInt.of(2))
        assertThat(datatype.length).isEmpty
        assertThat(datatype.category).contains(Category.NUMERIC)
        val relation = hr.findRelation(id("HR"), id("\"EMPLOYEES\""))
        assertThat(relation.status).isEqualTo(Status.FOUND)
        assertThat(relation.columnName).isEmpty
        assertThat(relation.column).isEmpty
    }

    @Test
    fun everyLookupStatusFollowsDeclaredCoverage() {
        val partialCols = ProjectExternalRelation.of("APP", "LOG", false, listOf(col("MSG", 1)))
        val md = ProjectExternalMetadata.builder("o").completeSchema("HR").relation(employees).relation(partialCols).build()
        assertThat(ProjectExternalMetadata.none().findColumn(id("hr"), id("employees"), id("id")).status).isEqualTo(Status.UNAVAILABLE)
        assertThat(ProjectExternalMetadata.none().findRelation(id("hr"), id("employees")).origin).isEmpty
        // Relation absent: proven only under complete schema coverage.
        assertThat(md.findRelation(id("hr"), id("jobs")).status).isEqualTo(Status.RELATION_NOT_FOUND)
        assertThat(md.findColumn(id("hr"), id("jobs"), id("x")).status).isEqualTo(Status.RELATION_NOT_FOUND)
        assertThat(md.findRelation(id("app"), id("users")).status).isEqualTo(Status.NOT_COVERED)
        // A supplied relation is FOUND even with partial columns; a supplied column is FOUND whatever the coverage.
        assertThat(md.findRelation(id("app"), id("log")).status).isEqualTo(Status.FOUND)
        assertThat(md.findColumn(id("app"), id("log"), id("msg")).status).isEqualTo(Status.FOUND)
        // Column absent: proven only under complete column coverage; the relation stays as evidence.
        val missingComplete = md.findColumn(id("hr"), id("employees"), id("bonus"))
        assertThat(missingComplete.status).isEqualTo(Status.COLUMN_NOT_FOUND)
        assertThat(missingComplete.relation).contains(employees)
        val missingPartial = md.findColumn(id("app"), id("log"), id("level"))
        assertThat(missingPartial.status).isEqualTo(Status.NOT_COVERED)
        assertThat(missingPartial.relation).contains(partialCols)
        assertThat(missingPartial.origin).contains("o")
        assertThat(Status.entries.map { it.name }).containsExactly("FOUND", "RELATION_NOT_FOUND", "COLUMN_NOT_FOUND", "NOT_COVERED", "UNAVAILABLE")
    }

    @Test
    fun emptyCompleteRelationIsKnownAndProvesColumnAbsence() {
        val empty = ProjectExternalRelation.of("HR", "EMPTY_T", true, emptyList())
        val md = ProjectExternalMetadata.builder("o").relation(empty).build()
        assertThat(md.findRelation(id("hr"), id("empty_t")).relation.get().rowFields).isEmpty()
        assertThat(md.findColumn(id("hr"), id("empty_t"), id("x")).status).isEqualTo(Status.COLUMN_NOT_FOUND)
        val unknownColumns = ProjectExternalRelation.of("HR", "UNKNOWN_T", false, emptyList())
        assertThat(unknownColumns.isColumnsComplete).isFalse()
    }

    @Test
    fun dictionaryNamesKeepOracleIdentity() {
        val mixed = ProjectExternalRelation.of("hr", "Emp", true, listOf(col("Sal x", 1), col("a.b", 2), col("lower", 3)))
        val md = ProjectExternalMetadata.builder("o").relation(mixed).relation(ProjectExternalRelation.of("HR", "EMP", true, emptyList())).build()
        // Dictionary "hr" is a case-sensitive schema distinct from HR.
        assertThat(md.findRelation(id("\"hr\""), id("\"Emp\"")).relation).contains(mixed)
        assertThat(md.findRelation(id("hr"), id("\"Emp\"")).status).isEqualTo(Status.NOT_COVERED)
        assertThat(md.findRelation(id("\"hr\""), id("emp")).status).isEqualTo(Status.NOT_COVERED)
        assertThat(md.findRelation(id("hr"), id("emp")).relation.get().schema).isEqualTo(id("HR"))
        assertThat(md.findColumn(id("\"hr\""), id("\"Emp\""), id("\"Sal x\"")).status).isEqualTo(Status.FOUND)
        assertThat(md.findColumn(id("\"hr\""), id("\"Emp\""), id("\"a.b\"")).status).isEqualTo(Status.FOUND)
        assertThat(md.findColumn(id("\"hr\""), id("\"Emp\""), id("lower")).status).isEqualTo(Status.COLUMN_NOT_FOUND)
        assertThat(mixed.qualifiedName.segments).hasSize(2)
        assertThat(mixed.columns[1].name.originalSpelling).isEqualTo("\"a.b\"")
        assertThat(ProjectExternalColumn.of("EMPLOYEES", 1, null).name).isEqualTo(id("employees")).isEqualTo(id("\"EMPLOYEES\""))
        assertThat(ProjectExternalColumn.of("Emp", 1, null).name).isEqualTo(id("\"Emp\"")).isNotEqualTo(id("emp"))
        assertThatThrownBy { ProjectExternalColumn.of("a\"b", 1, null) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ProjectExternalColumn.of("", 1, null) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun invisibleColumnsAreFoundButAreNotRowFields() {
        val t = ProjectExternalRelation.of("HR", "T", true, listOf(col("INV", null), col("B", 2), col("A", 1)))
        assertThat(t.rowFields.map { it.name }).containsExactly(id("a"), id("b"))
        assertThat(t.columns.map { it.name }).containsExactly(id("a"), id("b"), id("inv"))
        assertThat(t.columns[2].position).isEmpty
        val md = ProjectExternalMetadata.builder("o").relation(t).build()
        assertThat(md.findColumn(id("hr"), id("t"), id("inv")).status).isEqualTo(Status.FOUND)
    }

    @Test
    fun systemPrefixedNamesAreOutsideTheColumnCoverageContract() {
        val t = ProjectExternalRelation.of("HR", "T", true, listOf(col("SYS_FOO", 1), col("A", 2)))
        val md = ProjectExternalMetadata.builder("o").relation(t).build()
        assertThat(md.findColumn(id("hr"), id("t"), id("sys_nc00007$")).status).isEqualTo(Status.NOT_COVERED)
        assertThat(md.findColumn(id("hr"), id("t"), id("\"SYS_C00006_26101222:08:40$\"")).status).isEqualTo(Status.NOT_COVERED)
        assertThat(md.findColumn(id("hr"), id("t"), id("sys_foo")).status).isEqualTo(Status.FOUND)
        // Only the exact uppercase prefix of a normalized name is system-generated; a quoted lowercase name is in contract.
        assertThat(md.findColumn(id("hr"), id("t"), id("\"sys_x\"")).status).isEqualTo(Status.COLUMN_NOT_FOUND)
    }

    @Test
    fun objectTablesKeepTheirRowTypeAndOnlySuppliedAttributes() {
        val rowType = type("EMP_OBJ_T", owner = "HR")
        val complete = ProjectExternalRelation.objectTable("HR", "EMP_OT", rowType, true, listOf(col("B", 2), col("A", 1)))
        assertThat(complete.rowObjectType).contains(rowType)
        assertThat(complete.rowFields.map { it.name }).containsExactly(id("a"), id("b"))
        assertThat(employees.rowObjectType).isEmpty
        val partial = ProjectExternalRelation.objectTable("HR", "EMP_OT2", rowType, false, listOf(col("A", 1)))
        val md = ProjectExternalMetadata.builder("o").relation(complete).relation(partial).build()
        assertThat(md.findColumn(id("hr"), id("emp_ot"), id("c")).status).isEqualTo(Status.COLUMN_NOT_FOUND)
        assertThat(md.findColumn(id("hr"), id("emp_ot2"), id("c")).status).isEqualTo(Status.NOT_COVERED)
        assertThat(complete).isNotEqualTo(ProjectExternalRelation.of("HR", "EMP_OT", true, listOf(col("A", 1), col("B", 2))))
    }

    @Test
    fun datatypesKeepNullApartFromZeroAndPreserveDictionaryValues() {
        fun d(t: ProjectExternalDatatype) = listOf(t.length, t.lengthSemantics, t.precision, t.scale, t.category)
        val e = OptionalInt.empty()
        val none = Optional.empty<Any>()
        assertThat(d(type("NUMBER", precision = 8, scale = 0))).containsExactly(e, none, OptionalInt.of(8), OptionalInt.of(0), Optional.of(Category.NUMERIC))
        assertThat(d(type("NUMBER"))).containsExactly(e, none, e, e, Optional.of(Category.NUMERIC))
        assertThat(d(type("NUMBER", scale = 2))).containsExactly(e, none, e, OptionalInt.of(2), Optional.of(Category.NUMERIC))
        assertThat(type("NUMBER", precision = 5, scale = -2).scale).isEqualTo(OptionalInt.of(-2))
        assertThat(type("NUMBER", precision = 8, scale = 0)).isNotEqualTo(type("NUMBER", precision = 8))
        assertThat(d(type("FLOAT", precision = 10))).containsExactly(e, none, OptionalInt.of(10), e, Optional.of(Category.NUMERIC))
        assertThat(d(type("VARCHAR2", length = 30, semantics = BYTE))).containsExactly(OptionalInt.of(30), Optional.of(BYTE), e, e, Optional.of(Category.CHARACTER))
        assertThat(type("VARCHAR2", length = 30, semantics = CHAR)).isNotEqualTo(type("VARCHAR2", length = 30, semantics = BYTE))
        assertThat(type("NVARCHAR2", length = 10, semantics = CHAR).category).contains(Category.CHARACTER)
        assertThat(type("RAW", length = 16, semantics = BYTE).category).contains(Category.CHARACTER)
        assertThat(d(type("TIMESTAMP(3)", scale = 3))).containsExactly(e, none, e, OptionalInt.of(3), Optional.of(Category.DATE))
        assertThat(type("TIMESTAMP(6) WITH TIME ZONE", scale = 6).category).contains(Category.DATE)
        assertThat(d(type("INTERVAL DAY(2) TO SECOND(6)", precision = 2, scale = 6))).containsExactly(e, none, OptionalInt.of(2), OptionalInt.of(6), Optional.of(Category.DATE))
        assertThat(type("DATE").category).contains(Category.DATE)
        assertThat(type("CLOB").category).contains(Category.LOB)
        assertThat(type("TIMESTAMP WITH LOCAL TIME ZONE").category).contains(Category.DATE)
        assertThat(type("INTERVAL YEAR(2) TO MONTH", precision = 2, scale = 0).category).contains(Category.DATE)
        // Names that merely start with a datetime keyword are not datetime types.
        assertThat(type("TIMESTAMP_EXTRA").category).isEmpty
        assertThat(type("INTERVAL_CUSTOM(3)").category).isEmpty
        assertThat(type("Emp_T", owner = "hr").toString()).isEqualTo("\"hr\".Emp_T")
        // Known but uncategorized types keep their identity.
        val obj = type("EMP_OBJ_T", owner = "HR")
        assertThat(obj.category).isEmpty
        assertThat(obj.typeOwner).contains(id("hr"))
        assertThat(type("XMLTYPE", owner = "PUBLIC").category).isEmpty
        assertThat(type("NUMBER", owner = "HR").category).isEmpty
        assertThat(type("LONG RAW").category).isEmpty
        assertThat(type("VECTOR").category).isEmpty
        assertThat(type("XMLTYPE", owner = "PUBLIC").name).isEqualTo("XMLTYPE")
    }

    @Test
    fun unknownDatatypeIsDistinctFromUncategorizedType() {
        val unknown = col("X", 1, null)
        assertThat(unknown.datatype).isEmpty
        val uncategorized = col("Y", 2, type("ANYDATA", owner = "SYS"))
        assertThat(uncategorized.datatype.get().category).isEmpty
    }

    @Test
    fun invalidArgumentsAreRejected() {
        assertThatThrownBy { type(" ") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { type("VARCHAR2", semantics = CHAR) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { type("VARCHAR2", length = -1) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { type("NUMBER", precision = -1) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { type("T", owner = " ") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { col("A", 0) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ProjectExternalRelation.of("HR", "T", true, listOf(col("A", 1), col("A", 2))) }
            .isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("Duplicate column name")
        // Dictionary names are exact: "A" and "a" are two different columns.
        assertThat(ProjectExternalRelation.of("HR", "T", true, listOf(col("A", 1), col("a", 2))).columns).hasSize(2)
        assertThatThrownBy { ProjectExternalRelation.of("HR", "T", true, listOf(col("A", 1), col("B", 1))) }
            .isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("Duplicate column position")
        // Several invisible columns have no position and are not duplicates.
        assertThat(ProjectExternalRelation.of("HR", "T", true, listOf(col("A", null), col("B", null))).rowFields).isEmpty()
        assertThatThrownBy { ProjectExternalMetadata.builder(" ") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ProjectExternalMetadata.builder("o").relation(employees).relation(ProjectExternalRelation.of("HR", "EMPLOYEES", false, emptyList())) }
            .isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("Duplicate external relation")
        // Same name in another schema is a different relation.
        ProjectExternalMetadata.builder("o").relation(employees).relation(ProjectExternalRelation.of("APP", "EMPLOYEES", true, emptyList())).build()
    }

    @Test
    fun snapshotsAreDefensiveCopies() {
        val columns = mutableListOf(col("A", 1))
        val relation = ProjectExternalRelation.of("HR", "T", true, columns)
        columns += col("B", 2)
        assertThat(relation.columns).hasSize(1)
        assertThatThrownBy { (relation.columns as MutableList<ProjectExternalColumn>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { (relation.rowFields as MutableList<ProjectExternalColumn>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)

        val builder = ProjectExternalMetadata.builder("o").relation(relation)
        val first = builder.build()
        builder.completeSchema("HR").relation(ProjectExternalRelation.of("HR", "U", true, emptyList()))
        val second = builder.build()
        assertThat(first.findRelation(id("hr"), id("u")).status).isEqualTo(Status.NOT_COVERED)
        assertThat(first.findRelation(id("hr"), id("v")).status).isEqualTo(Status.NOT_COVERED)
        assertThat(second.findRelation(id("hr"), id("u")).status).isEqualTo(Status.FOUND)
        assertThat(second.findRelation(id("hr"), id("v")).status).isEqualTo(Status.RELATION_NOT_FOUND)
        // The built snapshot does not reference the builder or its mutable collections.
        val fields = ProjectExternalMetadata::class.java.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }
        for (field in fields) {
            field.isAccessible = true
            val value = field.get(second)
            assertThat(value).isNotInstanceOf(ProjectExternalMetadata.Builder::class.java)
            if (value is MutableCollection<*>) assertThatThrownBy { value.clear() }.isInstanceOf(UnsupportedOperationException::class.java)
            if (value is MutableMap<*, *>) assertThatThrownBy { value.clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        }
    }

    @Test
    fun equalDescriptorsAreEqualAndLookupsAreDeterministic() {
        assertThat(ProjectExternalRelation.of("HR", "EMPLOYEES", true, listOf(salaryCol, idCol, nameCol))).isEqualTo(employees).hasSameHashCodeAs(employees)
        assertThat(col("A", 1)).isEqualTo(col("A", 1)).isNotEqualTo(col("A", 2))
        assertThat(ProjectExternalMetadata.none()).isSameAs(ProjectExternalMetadata.none())
        val pool = Executors.newFixedThreadPool(8)
        try {
            val names = listOf("id", "salary", "name", "bonus")
            val tasks = (1..200).map { i ->
                Callable { names[i % 4] to hr.findColumn(id("hr"), id("employees"), id(names[i % 4])).status }
            }
            val results = pool.invokeAll(tasks).map { it.get() }.toSet()
            assertThat(results).containsExactlyInAnyOrder(
                "id" to Status.FOUND, "salary" to Status.FOUND, "name" to Status.FOUND, "bonus" to Status.COLUMN_NOT_FOUND
            )
        } finally {
            pool.shutdownNow()
        }
    }
}
