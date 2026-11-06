//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.project.OracleIdentifier
import com.felipebz.zpa.project.ProjectCallArgument
import com.felipebz.zpa.project.ProjectCallArguments

/** The structural arguments of an `ARGUMENTS` node: positional, or named by `=>`. */
internal fun callArguments(arguments: AstNode): ProjectCallArguments =
    ProjectCallArguments.of(arguments.getChildren(PlSqlGrammar.ARGUMENT).map { argument ->
        if (argument.hasDirectChildren(PlSqlPunctuator.ASSOCIATION)) ProjectCallArgument.named(OracleIdentifier.fromSource(argument.firstChild.tokenOriginalValue))
        else ProjectCallArgument.positional()
    })
