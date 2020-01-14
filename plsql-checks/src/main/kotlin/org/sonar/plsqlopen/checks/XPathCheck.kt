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
import com.sonar.sslr.xpath.api.AstNodeXPathQuery
import org.sonar.plsqlopen.squid.AnalysisException
import org.sonar.plugins.plsqlopen.api.annotations.*

@Rule(key = XPathCheck.CHECK_KEY, priority = Priority.MAJOR)
@RuleInfo(scope = RuleInfo.Scope.ALL)
@RuleTemplate
class XPathCheck : AbstractBaseCheck() {

    @RuleProperty(key = "xpathQuery")
    var xpathQuery = DEFAULT_XPATH_QUERY

    @RuleProperty(key = "message", defaultValue = "" + DEFAULT_MESSAGE)
    var message = DEFAULT_MESSAGE

    private var query: AstNodeXPathQuery<Any>? = null

    private fun query(): AstNodeXPathQuery<Any>? {
        if (query == null && xpathQuery.isNotEmpty()) {
            try {
                query = AstNodeXPathQuery.create(xpathQuery)
            } catch (e: RuntimeException) {
                throw AnalysisException(
                        "Unable to initialize the XPath engine, perhaps because of an invalid query: $xpathQuery", e)
            }

        }
        return query
    }

    override fun visitFile(node: AstNode) {
        if (query() != null) {
            val objects = query()?.selectNodes(node) ?: emptyList()

            for (obj in objects) {
                (obj as? AstNode)?.let { addIssue(it, message) }
                        ?: if (obj is Boolean && obj) {
                            addFileIssue(message)
                        }
            }
        }
    }

    companion object {
        internal const val CHECK_KEY = "XPath"

        private const val DEFAULT_XPATH_QUERY = ""
        private const val DEFAULT_MESSAGE = "The XPath expression matches this piece of code"
    }

}
