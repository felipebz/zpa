/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2010-2021 SonarSource SA
 * Copyright (C) 2021-2021 Felipe Zorzo
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
 * Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import java.awt.Point
import java.io.File

/**
 * Contract interface for the view.
 *
 * Note that *none* of the methods here-under should generate *any* event back to the presenter.
 * Only end-user interactions are supposed to generate events.
 */
internal interface ToolkitView {
    /**
     * Launch the application.
     */
    fun run()

    /**
     * Set the title of the application.
     *
     * @param title
     */
    fun setTitle(title: String?)

    /**
     * Prompt the user for a file to parse and return it.
     *
     * @return The file to parse, or null if no file was picked
     */
    fun pickFileToParse(): File?

    /**
     * Display the given HTML highlighted source code in the source code editor.
     * Scrollbars state is undefined after a call to this method.
     *
     * @param newSourceCode The HTML highlighted source code
     */
    fun displaySourceCode(newSourceCode: String)

    /**
     * Display the abstract syntax tree view starting from a given node.
     *
     * @param astNode The root AST node or null if no abstract syntax tree must be shown
     */
    fun displayAst(astNode: AstNode?)

    /**
     * Display the given string in the XML view.
     *
     * @param xml The string to display
     */
    fun displayXml(xml: String)

    /**
     * Display the given scope.
     *
     * @param scope The scope to display
     */
    fun displayScope(scope: Scope?)

    fun displayStatistics(numberOfCharacters: Int, numberOfLines: Int?, numberOfTokens: Int, parseTime: Long)

    /**
     * Get the current source code editor scrollbars' position point.
     *
     * @return The point
     */
    val sourceCodeScrollbarPosition: Point

    /**
     * Scroll the source code editor in order to make the given point visible.
     *
     * @param point to make visible
     */
    fun scrollSourceCodeTo(point: Point)

    /**
     * Get the source code currently entered in the source code editor.
     *
     * @return The source code
     */
    val sourceCode: String

    /**
     * Get the text currently entered in the XPath field.
     *
     * @return The XPath field text
     */
    val xPath: String?

    /**
     * Select the given AST node in the abstract syntax tree view.
     *
     * @param astNode The AST node to select, null will lead to a no operation
     */
    fun selectAstNode(astNode: AstNode?)

    /**
     * Clear all the selections in the abstract syntax tree view.
     */
    fun clearAstSelections()

    /**
     * Scroll the abstract syntax tree view in order to make the given AST node visible.
     *
     * @param astNode The AST node to make visible, null will lead to a no operation
     */
    fun scrollAstTo(astNode: AstNode?)

    /**
     * Highlight the given AST node in the source code editor.
     *
     * @param astNode The AST node to highlight
     */
    fun highlightSourceCode(startToken: Token, endToken: Token)

    /**
     * Clear all the highlights in the source code editor.
     */
    fun clearSourceCodeHighlights()

    /**
     * Scroll the source code editor in order to make the given AST node visible.
     *
     * @param astNode The AST node to make visible, null will lead to a no operation
     */
    fun scrollSourceCodeTo(astNode: AstNode?)

    /**
     * Disable the XPath evaluate button.
     */
    fun disableXPathEvaluateButton()

    /**
     * Enable the XPath evaluate button.
     */
    fun enableXPathEvaluateButton()

    /**
     * Get the AST node which follows the current source code editor text cursor position.
     *
     * @return The following AST node, or null if there is no such node
     */
    val astNodeFollowingCurrentSourceCodeTextCursorPosition: AstNode?

    /**
     * Get the list of nodes currently selected in the abstract syntax tree view.
     *
     * @return The list of selected AST nodes
     */
    val selectedAstNodes: List<AstNode>

    val selectedSymbolOrScopeTrees: List<AstNode>

    /**
     * Append the given message to the console view.
     *
     * @param message The message to append
     */
    fun appendToConsole(message: String?)

    /**
     * Set the focus on the console view.
     */
    fun setFocusOnConsoleView()

    /**
     * Set the focus on the abstract syntax tree view.
     */
    fun setFocusOnAbstractSyntaxTreeView()

    /**
     * Clear the console.
     */
    fun clearConsole()

    /**
     * Add a new configuration property to the configuration tab.
     *
     * @param name
     * @param description
     */
    fun addConfigurationProperty(name: String, description: String)

    /**
     * Get the value currently entered in the configuration property field identified by the given name.
     *
     * @param name The name of the configuration property
     * @return The current value of the field
     */
    fun getConfigurationPropertyValue(name: String): String

    /**
     * Set the current value of the configuration property field identified by the given name.
     *
     * @param name The name of the configuration property
     * @param value The value to be set
     */
    fun setConfigurationPropertyValue(name: String, value: String)

    /**
     * Set the error message of the configuration property identified by the given name.
     *
     * @param name The name of the configuration property
     * @param errorMessage The error message
     */
    fun setConfigurationPropertyErrorMessage(name: String, errorMessage: String)

    /**
     * Set the focus on the configuration field identified by the given name.
     *
     * @param name
     */
    fun setFocusOnConfigurationPropertyField(name: String)

    /**
     * Set the focus on the configuration view.
     */
    fun setFocusOnConfigurationView()
}
