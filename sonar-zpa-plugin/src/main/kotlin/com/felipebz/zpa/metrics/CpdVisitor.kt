//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.metrics

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.cpd.NewCpdTokens

class CpdVisitor(context: SensorContext, inputFile: InputFile) : PlSqlCheck() {

    private val newCpdTokens: NewCpdTokens = context.newCpdTokens().onFile(inputFile)

    override fun init() {
        subscribeTo(PlSqlGrammar.ANONYMOUS_BLOCK,
                PlSqlGrammar.CREATE_PROCEDURE,
                PlSqlGrammar.CREATE_FUNCTION,
                PlSqlGrammar.CREATE_PACKAGE_BODY,
                PlSqlGrammar.CREATE_TYPE_BODY)
    }

    override fun visitNode(node: AstNode) {
        for (token in node.tokens) {
            saveCpdTokens(token)
        }
    }

    private fun saveCpdTokens(token: Token) {
        newCpdTokens.addToken(token.line, token.column, token.endLine, token.endColumn, token.value)
    }

    override fun leaveFile(node: AstNode) {
        newCpdTokens.save()
    }

}
