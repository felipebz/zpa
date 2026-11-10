//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.semantics.CallBinding
import com.felipebz.zpa.api.semantics.CallKeptLocalReason
import com.felipebz.zpa.api.semantics.LocalCallStatus
import com.felipebz.zpa.api.semantics.UsageUnavailability

/** The decisions of [LocalForwardCalls] for the file's calls, answered by the node of each call's name. */
internal class LocalCallBindings(private val facts: LocalSemanticFacts) {
    private val callsByStart = facts.calls.associateBy { it.reference.range.startLine to it.reference.range.startColumn }

    fun binding(name: AstNode): CallBinding {
        val written = when (name.type) {
            PlSqlGrammar.VARIABLE_NAME -> 0
            // `a.b.c`: the names after the first; an attribute or a database link is no call name.
            PlSqlGrammar.MEMBER_EXPRESSION -> name.children.count { it.type === PlSqlGrammar.IDENTIFIER_NAME }
            else -> return unavailable()
        }
        val call = callsByStart[name.token.line to name.token.column]?.takeIf { it.reference.segments.size == written } ?: return unavailable()
        val local = call.local
        return when {
            local != null -> CallBinding.Local(
                status(local.status), local.selected?.let(facts.names::declaration), local.candidates.map { facts.names.declaration(it.declaration) }
            )
            call.outcome == LocalCallOutcome.PROJECT -> call.typed?.let(CallBinding::Project) ?: unavailable()
            else -> CallBinding.KeptLocal(
                when (call.outcome) {
                    LocalCallOutcome.INCOMPLETE -> CallKeptLocalReason.INCOMPLETE
                    LocalCallOutcome.UNSUPPORTED -> CallKeptLocalReason.UNSUPPORTED
                    else -> CallKeptLocalReason.LOCAL_CAPTURE
                }
            )
        }
    }

    private fun status(status: LocalCallResolution.Status) = when (status) {
        LocalCallResolution.Status.RESOLVED -> LocalCallStatus.RESOLVED
        LocalCallResolution.Status.STATIC_ON_INSTANCE -> LocalCallStatus.STATIC_ON_INSTANCE
        LocalCallResolution.Status.MEMBER_FROM_STATIC -> LocalCallStatus.MEMBER_FROM_STATIC
        LocalCallResolution.Status.AMBIGUOUS -> LocalCallStatus.AMBIGUOUS
        LocalCallResolution.Status.NO_APPLICABLE_CANDIDATE -> LocalCallStatus.NO_APPLICABLE_CANDIDATE
        LocalCallResolution.Status.NO_COMPATIBLE_CANDIDATE -> LocalCallStatus.NO_COMPATIBLE_CANDIDATE
        LocalCallResolution.Status.INVALID_NOTATION -> LocalCallStatus.INVALID_NOTATION
        LocalCallResolution.Status.INCOMPLETE -> LocalCallStatus.INCOMPLETE
    }

    private fun unavailable() = CallBinding.Unavailable(UsageUnavailability.NOT_A_COVERED_NAME)
}
