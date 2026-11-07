//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(com.felipebz.zpa.api.annotations.ZpaExperimentalApi::class)
@file:Suppress("DEPRECATION")

package com.felipebz.zpa.symbols

import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.PlSqlType
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.datatype.RecordDatatype
import com.felipebz.zpa.api.symbols.datatype.UnknownDatatype
import com.felipebz.zpa.project.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EffectiveSemanticTypeQueryTest {
    @Test
    fun legacyRecordTypeIsReportedAsRecord() {
        val node = node()
        node.plSqlDatatype = RecordDatatype(null, null, emptyList())

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.RECORD, EffectiveTypeSource.LEGACY)
        )
    }

    @Test
    fun projectRecordSymbolIsReportedAsRecordWithoutCreatingADatatype() {
        val node = node()
        val symbol = Symbol(null, Symbol.Kind.VARIABLE, ScopeImpl(), UnknownDatatype)
        symbol.projectTypeDeclaration = record("project_record")
        node.symbol = symbol

        assertThat(EffectiveSemanticTypeQuery.typeCategory(node)).isEqualTo(
            EffectiveTypeCategory.Known(PlSqlType.RECORD, EffectiveTypeSource.PROJECT_DECLARATION)
        )
        assertThat(node.plSqlDatatype).isSameAs(UnknownDatatype)
    }

    private fun node() = SemanticAstNode(PlSqlGrammar.DATATYPE, "DATATYPE", null)

    private fun record(name: String) = PackageTypeDeclaration(
        QualifiedName(identifier("p")),
        identifier(name),
        ProjectTypeShape.RECORD,
        emptyList(),
        FileId("$name.sql"),
        range("$name.sql")
    )

    private fun identifier(name: String) = com.felipebz.zpa.project.OracleIdentifier.fromSource(name)

    private fun range(file: String) = SourceRange(FileId(file), 1, 0, 1, 1)
}
