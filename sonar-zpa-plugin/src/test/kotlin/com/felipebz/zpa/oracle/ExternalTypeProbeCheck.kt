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
package com.felipebz.zpa.oracle

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.CustomPlSqlRulesDefinition
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.*
import java.util.*

/**
 * Test-only semantic check: describes the `%TYPE`/`%ROWTYPE` of every package value declared by the package it
 * visits, with the run's external context, and reports the description as an issue message, e.g.
 * `hr_api.v_salary RESOLVED NUMBER(10,2)`.
 */
@OptIn(ZpaExperimentalApi::class)
@Rule(key = ExternalTypeProbeCheck.KEY, name = "External type probe", description = "Test only", priority = Priority.INFO)
class ExternalTypeProbeCheck : PlSqlCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.CREATE_PACKAGE)
    }

    override fun visitNode(node: AstNode) {
        val analysis = projectAnalysis()
        val external = analysis.externalContext
        val snapshot = analysis.snapshot
        contexts.add(external)
        snapshots.add(snapshot)
        val packageName = node.getFirstChild(PlSqlGrammar.UNIT_NAME).getDescendants(PlSqlGrammar.IDENTIFIER_NAME)
            .joinToString(".") { it.tokenOriginalValue }.lowercase()
        snapshot.declarations.filterIsInstance<ProjectPackageValueView>()
            .filter { it.owner.toString().lowercase() == packageName }
            .forEach { value ->
                val description = snapshot.describeExternalType(
                    value.typeRef, value.owner, external.metadata, external.compilationSchema(value.owner).orElse(null)
                )
                addIssue(node, "${value.qualifiedName} ${render(description)}")
            }
    }

    class Definition : CustomPlSqlRulesDefinition() {
        override fun repositoryName() = "ZPA test"
        override fun repositoryKey() = REPOSITORY
        override fun checkClasses(): Array<Class<*>> = arrayOf(ExternalTypeProbeCheck::class.java)
    }

    companion object {
        const val REPOSITORY = "zpa-test"
        const val KEY = "ExternalTypeProbe"

        /** Every external context and snapshot seen by any instance, compared by identity. */
        val contexts: MutableSet<ProjectExternalContext> = Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap()))
        val snapshots: MutableSet<ProjectSnapshot> = Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap()))

        fun reset() {
            contexts.clear()
            snapshots.clear()
        }

        fun render(description: ProjectExternalTypeDescription): String {
            if (description.status != ProjectExternalTypeDescription.Status.RESOLVED) return description.status.name
            val lookup = description.lookup.get()
            return if (description.anchor.orElse(null) == ProjectTypeReference.Anchor.ROWTYPE) {
                "RESOLVED " + lookup.relation.get().rowFields.joinToString(", ", "(", ")") {
                    "${it.name.originalSpelling.removeSurrounding("\"")} ${it.datatype.map(::render).orElse("?")}"
                }
            } else {
                "RESOLVED " + lookup.column.get().datatype.map(::render).orElse("?")
            }
        }

        private fun render(type: ProjectExternalDatatype): String = when {
            type.length.isPresent -> "${type.name}(${type.length.asInt}${type.lengthSemantics.map { " $it" }.orElse("")})"
            type.precision.isPresent && type.scale.isPresent -> "${type.name}(${type.precision.asInt},${type.scale.asInt})"
            type.precision.isPresent -> "${type.name}(${type.precision.asInt})"
            else -> type.name
        }
    }
}
