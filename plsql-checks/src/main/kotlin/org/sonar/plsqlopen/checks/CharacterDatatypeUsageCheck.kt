/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2020 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plsqlopen.checks

import com.sonar.sslr.api.AstNode
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(key = CharacterDatatypeUsageCheck.CHECK_KEY, priority = Priority.MINOR, tags = [Tags.OBSOLETE])
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
            addIssue(node, getLocalizedMessage(CHECK_KEY), datatype.tokenValue)
        }
    }

    companion object {
        internal const val CHECK_KEY = "CharacterDatatypeUsage"
        val CHAR_DATATYPE = arrayOf(PlSqlKeyword.CHAR, PlSqlKeyword.VARCHAR)
    }

}
