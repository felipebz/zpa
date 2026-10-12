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

/** Discovers public occurrences using exact indexed names, without datatype or lexical resolution. */
internal class ProjectCallCandidateFinder(private val context: ProjectAnalysisContext) {
    fun find(target: ProjectCallTarget): ProjectCallCandidates {
        val prepared = context.state as? ProjectAnalysisContext.State.Prepared
            ?: return ProjectCallCandidates(
                ProjectCallCandidates.Status.NOT_PREPARED, target, emptyList(), emptySet(),
                emptyList(), emptyList(), false
            )
        val index = prepared.result.index
        val owner = target.memberPackageName
        val packageEvidence = owner != null && index.hasPackageEvidence(owner)
        val memberInPlay = owner != null &&
            (target.form == ProjectCallTarget.Form.PACKAGE_MEMBER || packageEvidence)
        val specifications = if (memberInPlay) {
            index.findOccurrences(requireNotNull(owner)).filter {
                it.declaration is PackageDeclaration && it.declaration.role == DeclarationRole.SPECIFICATION
            }
        } else emptyList()
        val standalone = if (target.permitsStandalone) {
            index.findOccurrences(target.name).filter {
                it.declaration is StandaloneProcedureDeclaration || it.declaration is StandaloneFunctionDeclaration
            }
        } else emptyList()
        // An identified standalone heading without a fact is namespace evidence and incomplete standalone coverage.
        val standaloneOmitted = target.permitsStandalone && index.hasOmittedStandaloneCallable(target.name)
        val standaloneInPlay = target.permitsStandalone &&
            (target.form == ProjectCallTarget.Form.STANDALONE || standalone.isNotEmpty() || standaloneOmitted)
        val candidates = ArrayList<IndexedDeclaration>()
        if (memberInPlay && specifications.isNotEmpty()) {
            index.findSubprogramOccurrences(requireNotNull(owner), target.name.last).forEach {
                if (it.declaration.role == DeclarationRole.SPECIFICATION) candidates.add(it)
            }
        }
        candidates.addAll(standalone)
        val uncertainties = callUncertainties(target, specifications, memberInPlay, standaloneInPlay, standaloneOmitted)
        return ProjectCallCandidates(
            callDiscoveryStatus(candidates, uncertainties, specifications, prepared.result.failures, memberInPlay, standaloneInPlay),
            target, candidates, uncertainties, specifications, prepared.result.failures, packageEvidence
        )
    }
}
