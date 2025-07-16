/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
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
package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.typeIs
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.datatype.*

class SymbolVisitor(private val typeSolver: DefaultTypeSolver, private val globalScope: Scope? = null) : PlSqlCheck() {

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
        PlSqlGrammar.FORALL_STATEMENT,
        DmlGrammar.SELECT_EXPRESSION)

    val symbolTable = SymbolTableImpl()
    private lateinit var fileScope: Scope
    private var currentScope: Scope? = null

    val symbols: List<Symbol> = symbolTable.symbols

    override fun init() {
        subscribeTo(*scopeHolders)
        fileScope = ScopeImpl(globalScope = globalScope)
        symbolTable.addScope(fileScope)
        currentScope = fileScope
    }

    override fun visitFile(node: AstNode) {
        visit(node)

        context.symbolTable = symbolTable

        if (globalScope != null) {
            for (innerScope in fileScope.innerScopes) {
                declareGlobalSymbols(innerScope, globalScope)
            }
        }
    }

    private fun declareGlobalSymbols(current: Scope, outer: Scope) {
        val scope = ScopeImpl(
            outer = outer,
            firstToken = current.firstToken,
            lastToken = current.lastToken,
            isAutonomousTransaction = current.isAutonomousTransaction,
            hasExceptionHandler = current.hasExceptionHandler,
            isOverridingMember = current.isOverridingMember,
            identifier = current.identifier,
            type = current.type,
            plSqlFile = context.plSqlFile()
        )

        for (symbol in current.symbols.filter { it.isGlobal }) {
            val newSymbol = Symbol(null, symbol.kind, scope, symbol.datatype, symbol.name)
            scope.addSymbol(newSymbol)
        }

        for (innerScope in current.innerScopes.filter { it.isGlobal }) {
            declareGlobalSymbols(innerScope, scope)
        }
    }

    override fun visitNode(node: AstNode) {
        if (node.typeIs(scopeHolders)) {
            context.currentScope = symbolTable.getScopeFor(node)
        }
    }

    override fun leaveNode(node: AstNode) {
        if (node.typeIs(scopeHolders)) {
            context.currentScope = context.currentScope?.outer
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
        } else if (node.type === PlSqlGrammar.EXCEPTION_DECLARATION) {
            visitExceptionDeclaration(node)
        } else if (node.type === PlSqlGrammar.CUSTOM_SUBTYPE) {
            visitCustomSubtypeDeclaration(node)
        } else if (node.type === PlSqlGrammar.TABLE_OF_DECLARATION) {
            visitAssociativeArrayDeclaration(node)
        } else if (node.type === PlSqlGrammar.RECORD_DECLARATION) {
            visitRecordDeclaration(node)
        } else if (node.type === PlSqlGrammar.VARIABLE_NAME) {
            visitVariableName(node)
        } else if (node.type === PlSqlGrammar.MEMBER_EXPRESSION) {
            visitMemberExpression(node)
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
        } else if (node.type === DmlGrammar.SELECT_EXPRESSION) {
            visitSelectExpression(node)
        }
    }

    private fun visitUnit(node: AstNode) {
        val autonomousTransaction = node
            .getFirstChildOrNull(PlSqlGrammar.DECLARE_SECTION)
            ?.getChildren(PlSqlGrammar.PRAGMA_DECLARATION)
            ?.any { it.hasDirectChildren(PlSqlGrammar.AUTONOMOUS_TRANSACTION_PRAGMA) }

        val exceptionHandler = node
            .getFirstChildOrNull(PlSqlGrammar.STATEMENTS_SECTION)
            ?.hasDirectChildren(PlSqlGrammar.EXCEPTION_HANDLERS)

        val inheritanceClause = node.parent.getFirstChildOrNull(PlSqlGrammar.INHERITANCE_CLAUSE)

        val isOverridingMember = inheritanceClause != null &&
            inheritanceClause.firstChild.type !== PlSqlKeyword.NOT &&
            inheritanceClause.hasDirectChildren(PlSqlKeyword.OVERRIDING)

        val nodeType: AstNodeType
        val identifier = if (node.parent.type == PlSqlGrammar.CREATE_TRIGGER) {
            nodeType = PlSqlGrammar.CREATE_TRIGGER
            node.parent.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.UNIT_NAME)
        } else {
            nodeType = node.type
            node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.UNIT_NAME)
        }

        val datatype = node.getFirstChildOrNull(PlSqlKeyword.RETURN)?.nextSibling
        val type = if (datatype != null) solveType(datatype) else UnknownDatatype
        val symbolKind = when(node.type) {
            PlSqlGrammar.CREATE_PROCEDURE -> Symbol.Kind.PROCEDURE
            PlSqlGrammar.PROCEDURE_DECLARATION -> Symbol.Kind.PROCEDURE

            PlSqlGrammar.CREATE_FUNCTION -> Symbol.Kind.FUNCTION
            PlSqlGrammar.FUNCTION_DECLARATION -> Symbol.Kind.FUNCTION

            PlSqlGrammar.SIMPLE_DML_TRIGGER -> Symbol.Kind.TRIGGER
            PlSqlGrammar.INSTEAD_OF_DML_TRIGGER -> Symbol.Kind.TRIGGER
            PlSqlGrammar.COMPOUND_DML_TRIGGER -> Symbol.Kind.TRIGGER
            PlSqlGrammar.SYSTEM_TRIGGER  -> Symbol.Kind.TRIGGER

            PlSqlGrammar.CREATE_TYPE -> Symbol.Kind.TYPE
            PlSqlGrammar.CREATE_TYPE_BODY -> Symbol.Kind.TYPE
            PlSqlGrammar.TYPE_CONSTRUCTOR  -> Symbol.Kind.TYPE
            else -> {
                throw IllegalArgumentException("Unknown unit type: ${node.type}")
            }
        }

        val symbol = createSymbol(identifier, symbolKind, type)
        enterScope(node, autonomousTransaction, exceptionHandler, isOverridingMember, identifier.tokenValue,
            nodeType)
        symbol.innerScope = currentScope
    }

    private fun visitPackage(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.UNIT_NAME)
        val packageScope = symbolTable.scopes.lastOrNull { it.identifier == identifier.tokenValue && it.type == PlSqlGrammar.CREATE_PACKAGE }
        if (packageScope != null) {
            currentScope = packageScope
            val symbol = currentScope?.getSymbol(identifier.tokenValue)
            if (symbol != null) {
                symbol.addUsage(identifier)
                semantic(node).symbol = symbol
            }
            enterScope(node, autonomousTransaction = false, exceptionHandler = false)
        } else {
            val symbol = createSymbol(identifier, Symbol.Kind.PACKAGE, UnknownDatatype)
            enterScope(node, autonomousTransaction = false, exceptionHandler = false)
            symbol.innerScope = currentScope
        }
    }

    private fun visitCursor(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val symbol = createSymbol(identifier, Symbol.Kind.CURSOR, UnknownDatatype)
        enterScope(node)
        symbol.innerScope = currentScope
    }

    private fun visitBlock(node: AstNode) {
        val exceptionHandler = node
            .getFirstChild(PlSqlGrammar.STATEMENTS_SECTION)
            .hasDirectChildren(PlSqlGrammar.EXCEPTION_HANDLERS)
        enterScope(node, exceptionHandler =  exceptionHandler)
    }

    private fun visitFor(node: AstNode) {
        enterScope(node)
        val iterator = node.getFirstChild(PlSqlGrammar.ITERATOR)
        val declarations = iterator.getChildren(PlSqlGrammar.ITERAND_DECLARATION)
        val control = iterator.getFirstChild(PlSqlGrammar.QUAL_ITERATION_CTL)

        val iteratorType = if (control.hasDirectChildren(PlSqlGrammar.STEPPED_CONTROL)) {
            NumericDatatype(node)
        } else if (control.hasDirectChildren(PlSqlGrammar.SINGLE_EXPRESSION_CONTROL) ||
                   control.hasDirectChildren(PlSqlGrammar.DYNAMIC_SQL)) {
            RowtypeDatatype()
        } else {
            UnknownDatatype
        }

        for (declaration in declarations) {
            val identifier = declaration.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
            val datatype = declaration.getFirstChildOrNull(PlSqlGrammar.DATATYPE)
            val type = if (datatype != null) solveType(datatype) else iteratorType
            createSymbol(identifier, Symbol.Kind.VARIABLE, type)
        }
    }

    private fun visitForAll(node: AstNode) {
        enterScope(node)
        val identifier = node.getFirstChild(PlSqlKeyword.FORALL).nextSibling
        createSymbol(identifier, Symbol.Kind.VARIABLE, NumericDatatype(node))
    }

    private fun visitVariableDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val datatype = node.getFirstChild(PlSqlGrammar.DATATYPE)

        val type = solveType(datatype)
        createSymbol(identifier, Symbol.Kind.VARIABLE, type)
    }

    private fun visitExceptionDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)

        createSymbol(identifier, Symbol.Kind.VARIABLE, ExceptionDatatype())
    }

    private fun visitCustomSubtypeDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val datatype = node.getFirstChild(PlSqlGrammar.DATATYPE)

        val type = solveType(datatype)
        createSymbol(identifier, Symbol.Kind.TYPE, type)
    }

    private fun visitAssociativeArrayDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val datatype = node.getFirstChild(PlSqlGrammar.NESTED_TABLE_DEFINITION).getFirstChild(PlSqlGrammar.DATATYPE)
        createSymbol(identifier, Symbol.Kind.TYPE, AssociativeArrayDatatype(node, currentScope, solveType(datatype)))
    }

    private fun visitRecordDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)

        val scope = currentScope ?: throw IllegalStateException("Cannot create a symbol without a scope.")

        val fields = node.getChildren(PlSqlGrammar.RECORD_FIELD_DECLARATION).map { field ->
            val fieldName = field.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
            val datatype = field.getFirstChild(PlSqlGrammar.DATATYPE)
            val type = solveType(datatype)
            Symbol(fieldName, Symbol.Kind.VARIABLE, scope, type)
        }

        createSymbol(identifier, Symbol.Kind.TYPE, RecordDatatype(node, currentScope, fields))
    }

    private fun visitParameterDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val datatype = node.getFirstChild(PlSqlGrammar.DATATYPE)

        val type = solveType(datatype)
        createSymbol(identifier, Symbol.Kind.PARAMETER, type).addModifiers(node.getChildren(PlSqlKeyword.IN, PlSqlKeyword.OUT))
    }

    private fun visitVariableName(node: AstNode) {
        val identifier = node.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)
        if (identifier != null && currentScope != null) {
            val symbol = currentScope?.getSymbol(identifier.tokenValue)
            if (symbol != null) {
                symbol.addUsage(identifier)
                semantic(node).symbol = symbol

                val parent = node.parent
                if (parent.type == PlSqlGrammar.METHOD_CALL) {
                    val isArrayAccess = symbol.datatype is AssociativeArrayDatatype && symbol.kind != Symbol.Kind.FUNCTION
                    semantic(parent).plSqlDatatype = if (isArrayAccess)
                        symbol.datatype.nestedType
                    else
                        symbol.datatype
                }
            }
        }
    }

    private fun visitMemberExpression(node: AstNode) {
        val parts = node.getChildren(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.VARIABLE_NAME)
        val path = parts.dropLast(1).map { it.tokenValue }.reversed()
        val identifier = parts.last()

        if (currentScope != null) {
            val symbol = currentScope?.getSymbol(identifier.tokenValue, path)
            if (symbol != null) {
                symbol.addUsage(identifier)
                semantic(node).symbol = symbol
            }
        }
    }

    private fun visitLiteral(node: AstNode) {
        (node as SemanticAstNode).plSqlDatatype = solveType(node)
    }

    private fun visitSelectExpression(node: AstNode) {
        enterScope(node)
    }

    private fun createSymbol(identifier: AstNode, kind: Symbol.Kind, plSqlDatatype: PlSqlDatatype): Symbol {
        val scope = currentScope
        if (scope == null) {
            throw IllegalStateException("Cannot create a symbol without a scope.")
        } else {
            val symbol = symbolTable.declareSymbol(identifier, kind, scope, plSqlDatatype)
            semantic(identifier).symbol = symbol
            return symbol
        }
    }

    private fun enterScope(node: AstNode,
                           autonomousTransaction: Boolean? = null,
                           exceptionHandler: Boolean? = null,
                           overridingMember: Boolean? = null,
                           identifier: String? = null,
                           type: AstNodeType? = null) {
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
                exception = this.hasExceptionHandler || (exceptionHandler == true)
            } else if (exceptionHandler != null) {
                exception = exceptionHandler
            }
        }

        val scope = ScopeImpl(currentScope, node, node.token, node.lastToken, autonomous, exception, isOverridingMember, identifier, type)
        symbolTable.addScope(scope)
        currentScope = scope
    }

    private fun leaveScope() {
        val scope = currentScope
        requireNotNull(scope) { "Current scope should never be null when calling method \"leaveScope\"" }

        currentScope = if (scope.type == PlSqlGrammar.CREATE_PACKAGE_BODY && scope.outer?.type == PlSqlGrammar.CREATE_PACKAGE){
            scope.outer?.outer
        } else {
            scope.outer
        }
    }

    private fun solveType(node: AstNode): PlSqlDatatype {
        return typeSolver.solve(node, currentScope)
    }

}
