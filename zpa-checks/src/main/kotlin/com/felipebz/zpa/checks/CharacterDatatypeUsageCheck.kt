//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.typeIs

@Rule(priority = Priority.MINOR, tags = [Tags.OBSOLETE])
@ConstantRemediation("5min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
class CharacterDatatypeUsageCheck : AbstractBaseCheck() {

    override fun init() {
        subscribeTo(PlSqlGrammar.CHARACTER_DATAYPE)
    }

    override fun visitNode(node: AstNode) {
        val datatype = node.firstChild
        if (datatype.typeIs(CHAR_DATATYPE)) {
            addIssue(node, getLocalizedMessage(), datatype.tokenValue)
        }
    }

    companion object {
        val CHAR_DATATYPE = arrayOf(PlSqlKeyword.CHAR, PlSqlKeyword.VARCHAR)
    }

}
