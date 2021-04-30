/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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
package org.sonar.plsqlopen.symbols

import com.sonar.sslr.api.AstNode
import com.sonar.sslr.api.AstNodeType
import org.sonar.plsqlopen.typeIs
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck
import org.sonar.plugins.plsqlopen.api.squid.SemanticAstNode
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.Symbol

class SymbolVisitor(private val typeSolver: DefaultTypeSolver?) : PlSqlCheck() {

    private val scopeHolders = arrayOf<AstNodeType>(
        PlSqlGrammar.CREATE_PROCEDURE,
        PlSqlGrammar.PROCEDURE_DECLARATION,
        PlSqlGrammar.CREATE_FUNCTION,
        PlSqlGrammar.FUNCTION_DECLARATION,
        PlSqlGrammar.CREATE_PACKAGE,
        PlSqlGrammar.CREATE_PACKAGE_BODY,
        PlSqlGrammar.SIMPLE_DML_TRIGGER,
        PlSqlGrammar.INSTEAD_OF_DML_TRIGGER,
        PlSqlGrammar.COMPOUND_DML_TRIGGER,
        PlSqlGrammar.SYSTEM_TRIGGER,
        PlSqlGrammar.TYPE_CONSTRUCTOR,
        PlSqlGrammar.CREATE_TYPE,
        PlSqlGrammar.CREATE_TYPE_BODY,
        PlSqlGrammar.BLOCK_STATEMENT,
        PlSqlGrammar.FOR_STATEMENT,
        PlSqlGrammar.CURSOR_DECLARATION,
        PlSqlGrammar.FORALL_STATEMENT)

    private var symbolTable = SymbolTableImpl()
    private var currentScope: Scope? = null

    val symbols: List<Symbol> = symbolTable.symbols

    override fun init() {
        subscribeTo(*scopeHolders)
    }

    override fun visitFile(node: AstNode) {
        visit(node)

        context.symbolTable = symbolTable
    }

    override fun visitNode(node: AstNode) {
        if (node.typeIs(scopeHolders)) {
            context.currentScope = symbolTable.getScopeFor(node)
        }
    }

    override fun leaveNode(node: AstNode) {
        if (node.typeIs(scopeHolders)) {
            context.currentScope = context.currentScope?.outer()
        }
    }

    override fun leaveFile(node: AstNode) {
        currentScope = null
    }

    private fun visit(ast: AstNode) {
        visitNodeInternal(ast)
        visitChildren(ast)

        if (ast.typeIs(scopeHolders)) {
            leaveScope()
        }
    }

    private fun visitChildren(ast: AstNode) {
        for (child in ast.children) {
            visit(child)
        }
    }

    private fun visitNodeInternal(node: AstNode) {
        if (node.type === PlSqlGrammar.VARIABLE_DECLARATION) {
            visitVariableDeclaration(node)
        } else if (node.type === PlSqlGrammar.CUSTOM_SUBTYPE) {
            visitCustomSubtypeDeclaration(node)
        } else if (node.type === PlSqlGrammar.TABLE_OF_DECLARATION) {
            visitAssociativeArrayDeclaration(node)
        } else if (node.type === PlSqlGrammar.RECORD_DECLARATION) {
            visitRecordDeclaration(node)
        } else if (node.type === PlSqlGrammar.VARIABLE_NAME) {
            visitVariableName(node)
        } else if (node.type === PlSqlGrammar.CURSOR_DECLARATION) {
            visitCursor(node)
        } else if (node.type === PlSqlGrammar.BLOCK_STATEMENT) {
            visitBlock(node)
        } else if (node.type === PlSqlGrammar.FOR_STATEMENT) {
            visitFor(node)
        } else if (node.type === PlSqlGrammar.FORALL_STATEMENT) {
            visitForAll(node)
        } else if (node.type === PlSqlGrammar.PARAMETER_DECLARATION || node.type === PlSqlGrammar.CURSOR_PARAMETER_DECLARATION) {
            visitParameterDeclaration(node)
        } else if (node.type === PlSqlGrammar.CREATE_PROCEDURE ||
                node.type === PlSqlGrammar.PROCEDURE_DECLARATION ||
                node.type === PlSqlGrammar.CREATE_FUNCTION ||
                node.type === PlSqlGrammar.FUNCTION_DECLARATION ||
                node.type === PlSqlGrammar.SIMPLE_DML_TRIGGER ||
                node.type === PlSqlGrammar.INSTEAD_OF_DML_TRIGGER ||
                node.type === PlSqlGrammar.COMPOUND_DML_TRIGGER ||
                node.type === PlSqlGrammar.SYSTEM_TRIGGER ||
                node.type === PlSqlGrammar.CREATE_TYPE ||
                node.type === PlSqlGrammar.CREATE_TYPE_BODY ||
                node.type === PlSqlGrammar.TYPE_CONSTRUCTOR) {
            visitUnit(node)
        } else if (node.type === PlSqlGrammar.CREATE_PACKAGE || node.type === PlSqlGrammar.CREATE_PACKAGE_BODY) {
            visitPackage(node)
        } else if (node.type === PlSqlGrammar.LITERAL) {
            visitLiteral(node)
        }
    }

    private fun visitUnit(node: AstNode) {
        val autonomousTransaction = node
            .getChildren(PlSqlGrammar.DECLARE_SECTION).asSequence()
            .flatMap { it.getChildren(PlSqlGrammar.PRAGMA_DECLARATION).asSequence() }
            .flatMap { it.getChildren(PlSqlGrammar.AUTONOMOUS_TRANSACTION_PRAGMA).asSequence() }.any()
        val exceptionHandler = node
                .getChildren(PlSqlGrammar.STATEMENTS_SECTION).asSequence()
                .flatMap { it.getChildren(PlSqlGrammar.EXCEPTION_HANDLER).asSequence() }.any()
        val inheritanceClause = node.parent.getFirstChild(PlSqlGrammar.INHERITANCE_CLAUSE)
        val isOverridingMember = inheritanceClause != null &&
            inheritanceClause.firstChild.type !== PlSqlKeyword.NOT &&
            inheritanceClause.hasDirectChildren(PlSqlKeyword.OVERRIDING)

        enterScope(node, autonomousTransaction, exceptionHandler, isOverridingMember)
    }

    private fun visitPackage(node: AstNode) {
        enterScope(node, autonomousTransaction = false, exceptionHandler = false)
    }

    private fun visitCursor(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        createSymbol(identifier, Symbol.Kind.CURSOR, PlSqlType.UNKNOWN)
        enterScope(node)
    }

    private fun visitBlock(node: AstNode) {
        val exceptionHandler = node
                .getChildren(PlSqlGrammar.STATEMENTS_SECTION).asSequence()
                .flatMap { it.getChildren(PlSqlGrammar.EXCEPTION_HANDLER).asSequence() }.any()
        enterScope(node, exceptionHandler =  exceptionHandler)
    }

    private fun visitFor(node: AstNode) {
        enterScope(node)
        val identifier = node.getFirstChild(PlSqlKeyword.FOR).nextSibling

        val type = if (node.hasDirectChildren(PlSqlPunctuator.RANGE)) {
            PlSqlType.NUMERIC
        } else {
            PlSqlType.ROWTYPE
        }

        createSymbol(identifier, Symbol.Kind.VARIABLE, type)
    }

    private fun visitForAll(node: AstNode) {
        enterScope(node)
        val identifier = node.getFirstChild(PlSqlKeyword.FORALL).nextSibling
        createSymbol(identifier, Symbol.Kind.VARIABLE, PlSqlType.NUMERIC)
    }

    private fun visitVariableDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val datatype = node.getFirstChild(PlSqlGrammar.DATATYPE)

        val type = solveType(datatype)
        createSymbol(identifier, Symbol.Kind.VARIABLE, type)
    }

    private fun visitCustomSubtypeDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val datatype = node.getFirstChild(PlSqlGrammar.DATATYPE)

        val type = solveType(datatype)
        createSymbol(identifier, Symbol.Kind.TYPE, type)
    }

    private fun visitAssociativeArrayDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        createSymbol(identifier, Symbol.Kind.TYPE, PlSqlType.ASSOCIATIVE_ARRAY)
    }

    private fun visitRecordDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        createSymbol(identifier, Symbol.Kind.TYPE, PlSqlType.RECORD)
    }

    private fun visitParameterDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val datatype = node.getFirstChild(PlSqlGrammar.DATATYPE)

        val type = solveType(datatype)
        createSymbol(identifier, Symbol.Kind.PARAMETER, type).addModifiers(node.getChildren(PlSqlKeyword.IN, PlSqlKeyword.OUT))
    }

    private fun visitVariableName(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        if (identifier != null && currentScope != null) {
            val symbol = currentScope?.getSymbol(identifier.tokenOriginalValue)
            if (symbol != null) {
                symbol.addUsage(identifier)
                semantic(node).symbol = symbol
            }
        }
    }

    private fun visitLiteral(node: AstNode) {
        (node as SemanticAstNode).plSqlType = solveType(node)
    }

    private fun createSymbol(identifier: AstNode, kind: Symbol.Kind, type: PlSqlType): Symbol {
        val scope = currentScope
        if (scope == null) {
            throw IllegalStateException("Cannot create a symbol without a scope.")
        } else {
            val symbol = symbolTable.declareSymbol(identifier, kind, scope, type)
            semantic(identifier).symbol = symbol
            return symbol
        }
    }

    private fun enterScope(node: AstNode,
                           autonomousTransaction: Boolean? = null,
                           exceptionHandler: Boolean? = null,
                           overridingMember: Boolean? = null) {
        var autonomous = false
        var exception = false
        val isOverridingMember = overridingMember ?: false

        with(currentScope) {
            if (autonomousTransaction != null) {
                autonomous = autonomousTransaction
            } else if (this != null) {
                autonomous = this.isAutonomousTransaction
            }

            if (this != null) {
                exception = this.hasExceptionHandler() || java.lang.Boolean.TRUE == exceptionHandler
            } else if (exceptionHandler != null) {
                exception = exceptionHandler
            }
        }

        val scope = ScopeImpl(currentScope, node, autonomous, exception, isOverridingMember)
        symbolTable.addScope(scope)
        currentScope = scope
    }

    private fun leaveScope() {
        val scope = currentScope
        requireNotNull(scope) { "Current scope should never be null when calling method \"leaveScope\"" }
        currentScope = scope.outer()
    }

    private fun solveType(node: AstNode?): PlSqlType {
        var type = PlSqlType.UNKNOWN
        if (typeSolver != null) {
            type = typeSolver.solve(node, currentScope)
        }
        return type
    }

}
