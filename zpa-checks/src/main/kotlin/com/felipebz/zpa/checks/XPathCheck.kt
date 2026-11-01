//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.xpath.api.AstNodeXPathQuery
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.squid.AnalysisException

@Rule(priority = Priority.MAJOR)
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
                if (obj is AstNode) {
                    addIssue(obj, message)
                } else if (obj is Boolean && obj) {
                    addFileIssue(message)
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_XPATH_QUERY = ""
        private const val DEFAULT_MESSAGE = "The XPath expression matches this piece of code"
    }

}
