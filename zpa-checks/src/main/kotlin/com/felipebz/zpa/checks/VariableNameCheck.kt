//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.syntax.SyntaxViews
import com.felipebz.zpa.api.syntax.VariableDeclaration
import java.util.regex.Pattern

@Rule(priority = Priority.MINOR)
@ConstantRemediation("10min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class VariableNameCheck : AbstractBaseCheck() {

    @RuleProperty(key = "regexp", defaultValue = DEFAULT_REGEXP)
    var regexp = DEFAULT_REGEXP

    private val pattern: Pattern by lazy {
        Pattern.compile(regexp)
    }

    override fun init() {
        subscribeTo(SyntaxViews.VARIABLE_DECLARATION, ::visitVariableDeclaration)
    }

    private fun visitVariableDeclaration(declaration: VariableDeclaration) {
        val name = declaration.name

        if (!pattern.matcher(name).matches()) {
            addIssue(declaration.nameAstNode, getLocalizedMessage(), name, regexp)
        }
    }

    companion object {
        private const val DEFAULT_REGEXP = "[a-zA-Z]([a-zA-Z0-9_]*[a-zA-Z0-9])?"
    }

}
