//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.parser

import com.felipebz.flr.api.Grammar
import com.felipebz.flr.impl.Parser
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.squid.SemanticFileNode
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.squid.PlSqlConfiguration

object PlSqlParser {
    fun create(conf: PlSqlConfiguration): Parser<Grammar> =
        Parser.builder(PlSqlGrammar.create(conf).build())
            .withLexer(PlSqlLexer.create(conf))
            .withNonTerminalNodeBuilder(SemanticFileNode::create)
            .withTerminalNodeBuilder(::SemanticAstNode)
            .build()
}
