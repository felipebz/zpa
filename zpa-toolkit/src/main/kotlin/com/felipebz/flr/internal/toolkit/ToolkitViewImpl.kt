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
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.Symbol
import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.geom.Rectangle2D
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.tree.*

internal class ToolkitViewImpl(@Transient val presenter: ToolkitPresenter) : JFrame(), ToolkitView {
    private val tabbedPane = JTabbedPane()
    private val astTree = JTree()
    private val astTreeScrollPane = JScrollPane(astTree)
    private val xmlTextArea = JTextArea()
    private val xmlScrollPane = JScrollPane(xmlTextArea)
    private val xmlPanel = JPanel(BorderLayout(10, 2))
    private val consoleTextArea = JTextArea()
    private val consoleScrollPane = JScrollPane(consoleTextArea)
    private val configurationInnerPanel = JPanel(GridBagLayout())
    private val configurationOuterPanel = JPanel(BorderLayout())
    private val configurationScrollPane = JScrollPane(configurationOuterPanel)
    private val configurationPropertiesPanels: MutableMap<String?, ConfigurationPropertyPanel> = HashMap()
    private val symbolTree = JTree()
    private val symbolTreeScrollPane = JScrollPane(symbolTree)
    private val sourceCodeLabel = JLabel(" Source Code")
    private val sourceCodeEditorPane = JTextArea()
    private val sourceCodeEditorScrollPane = JScrollPane(sourceCodeEditorPane)
    private val sourceCodeOpenButton = JButton()
    private val sourceCodeParseButton = JButton()
    private val sourceCodeButtonsPanel = JPanel()
    private val sourceCodePanel = JPanel(BorderLayout(0, 2))
    private val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sourceCodePanel, tabbedPane)
    private val southPanel = JPanel(BorderLayout(0, 2))
    private val xpathLabel = JLabel("  XPath query")
    private val xpathTextArea = JTextArea()
    private val xpathTextAreaScrollPane = JScrollPane(xpathTextArea)
    private val xpathPanel = JPanel(BorderLayout(10, 2))
    private val fileChooser = JFileChooser()
    private val xpathButton = JButton()
    private val xpathButtonPanel = JPanel()

    @Transient
    private lateinit var lineOffsets: LineOffsets

    @Transient
    private val highlighter = DefaultHighlightPainter(Color.LIGHT_GRAY)
    private var sourceCodeTextCursorMovedEventDisabled = false
    private var astSelectionEventDisabled = false

    init {
        initComponents()
    }

    private fun initComponents() {
        setSize(1000, 700)
        setLocationRelativeTo(null)
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout(0, 5)
        astTree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
        astTree.addTreeSelectionListener {
            if (!astSelectionEventDisabled) {
                presenter.onAstSelectionChanged()
            }
        }
        symbolTree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        symbolTree.addTreeSelectionListener {
            presenter.onSymbolSelectionChanged()
        }
        consoleTextArea.isEditable = false
        consoleTextArea.font = Font.decode("Monospaced")
        tabbedPane.tabPlacement = JTabbedPane.TOP
        tabbedPane.add("Abstract Syntax Tree", astTreeScrollPane)
        tabbedPane.add("XML", xmlPanel)
        tabbedPane.add("Console", consoleScrollPane)
        tabbedPane.add("Configuration", configurationScrollPane)
        tabbedPane.add("Scopes/Symbols", symbolTreeScrollPane)
        configurationOuterPanel.add(configurationInnerPanel, BorderLayout.NORTH)
        configurationOuterPanel.add(Box.createGlue(), BorderLayout.CENTER)
        sourceCodeEditorPane.font = Font.decode("Monospaced")
        (sourceCodeEditorPane.caret as DefaultCaret).updatePolicy = DefaultCaret.UPDATE_WHEN_ON_EDT
        sourceCodeEditorPane.document.addDocumentListener(object : DocumentListener {
            override fun removeUpdate(e: DocumentEvent) {
                presenter.onSourceCodeKeyTyped()
            }

            override fun insertUpdate(e: DocumentEvent) {
                presenter.onSourceCodeKeyTyped()
            }

            override fun changedUpdate(e: DocumentEvent) {
                presenter.onSourceCodeKeyTyped()
            }
        })
        sourceCodeEditorPane.addCaretListener {
            if (!sourceCodeTextCursorMovedEventDisabled) {
                presenter.onSourceCodeTextCursorMoved()
            }
        }
        sourceCodeOpenButton.text = "Open Source File"
        sourceCodeOpenButton.addActionListener { presenter.onSourceCodeOpenButtonClick() }
        sourceCodeParseButton.text = "Parse Source Code"
        sourceCodeParseButton.addActionListener { presenter.onSourceCodeParseButtonClick() }
        sourceCodeButtonsPanel.add(sourceCodeOpenButton)
        sourceCodeButtonsPanel.add(sourceCodeParseButton)
        sourceCodePanel.add(sourceCodeLabel, BorderLayout.NORTH)
        sourceCodePanel.add(sourceCodeEditorScrollPane, BorderLayout.CENTER)
        sourceCodePanel.add(sourceCodeButtonsPanel, BorderLayout.SOUTH)
        splitPane.dividerLocation = width / 2
        add(splitPane, BorderLayout.CENTER)
        xmlPanel.add(xmlScrollPane, BorderLayout.CENTER)
        xpathPanel.add(xpathLabel, BorderLayout.NORTH)
        xpathPanel.add(Box.createHorizontalGlue(), BorderLayout.WEST)
        xpathTextArea.text = "//IDENTIFIER"
        xpathTextArea.rows = 8
        xpathPanel.add(xpathTextAreaScrollPane, BorderLayout.CENTER)
        xpathPanel.add(Box.createHorizontalGlue(), BorderLayout.EAST)
        southPanel.add(xpathPanel, BorderLayout.NORTH)
        xpathButton.text = "Evaluate XPath"
        xpathButton.addActionListener { presenter.onXPathEvaluateButtonClick() }
        xpathButtonPanel.add(xpathButton)
        southPanel.add(xpathButtonPanel, BorderLayout.SOUTH)
        xmlPanel.add(southPanel, BorderLayout.SOUTH)
    }

    override fun run() {
        isVisible = true
    }

    override fun pickFileToParse(): File? {
        return if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile
        } else {
            null
        }
    }

    override fun displaySourceCode(newSourceCode: String) {
        try {
            sourceCodeTextCursorMovedEventDisabled = true
            sourceCodeEditorPane.text = newSourceCode
            lineOffsets = LineOffsets(newSourceCode)
        } finally {
            sourceCodeTextCursorMovedEventDisabled = false
        }
    }

    override fun displayAst(astNode: AstNode?) {
        if (astNode == null) {
            astTree.model = EMPTY_TREE_MODEL
        } else {
            val treeNode: TreeNode = getTreeNode(astNode)
            astTree.model = DefaultTreeModel(treeNode)
        }
    }

    override fun displayXml(xml: String) {
        xmlTextArea.text = xml
    }

    override fun displayScope(scope: Scope?) {
        if (scope == null) {
            symbolTree.model = EMPTY_TREE_MODEL
        } else {
            val treeNode = getScopeTreeNode(scope)
            symbolTree.model = DefaultTreeModel(treeNode)
        }
    }

    override val sourceCodeScrollbarPosition: Point
        get() {
            val x = sourceCodeEditorScrollPane.horizontalScrollBar.value
            val y = sourceCodeEditorScrollPane.verticalScrollBar.value
            return Point(x, y)
        }

    override fun scrollSourceCodeTo(point: Point) {
        // http://stackoverflow.com/questions/8789371/java-jtextpane-jscrollpane-de-activate-automatic-scrolling
        SwingUtilities.invokeLater {
            sourceCodeEditorScrollPane.horizontalScrollBar.value = point.x
            sourceCodeEditorScrollPane.verticalScrollBar.value = point.y
        }
    }

    override val sourceCode: String
        get() = sourceCodeEditorPane.text

    override val xPath: String
        get() = xpathTextArea.text

    override fun selectAstNode(astNode: AstNode?) {
        if (astNode != null) {
            try {
                astSelectionEventDisabled = true
                val treeNode = getAstTreeNodeWithGivenUserObject(astTree.model.root as DefaultMutableTreeNode, astNode)
                if (treeNode != null) {
                    astTree.selectionModel.addSelectionPath(TreePath(treeNode.path))
                }
            } finally {
                astSelectionEventDisabled = false
            }
        }
    }

    private fun getAstTreeNodeWithGivenUserObject(
        treeNode: DefaultMutableTreeNode,
        userObject: Any
    ): DefaultMutableTreeNode? {
        return if (treeNode.userObject == userObject) {
            treeNode
        } else {
            for (i in 0 until treeNode.childCount) {
                val treeNodeWithUserObject =
                    getAstTreeNodeWithGivenUserObject(treeNode.getChildAt(i) as DefaultMutableTreeNode, userObject)
                if (treeNodeWithUserObject != null) {
                    return treeNodeWithUserObject
                }
            }
            null
        }
    }

    override fun highlightSourceCode(startToken: Token, endToken: Token) {
        val startOffset = lineOffsets.getStartOffset(startToken)
        val endOffset = lineOffsets.getEndOffset(endToken)
        try {
            sourceCodeEditorPane.highlighter.addHighlight(startOffset, endOffset, highlighter)
        } catch (e: BadLocationException) {
            throw RuntimeException(e)
        }
    }

    override fun clearAstSelections() {
        try {
            astSelectionEventDisabled = true
            astTree.selectionModel.clearSelection()
        } finally {
            astSelectionEventDisabled = false
        }
    }

    override fun scrollAstTo(astNode: AstNode?) {
        if (astNode != null) {
            val treeNode = getAstTreeNodeWithGivenUserObject(astTree.model.root as DefaultMutableTreeNode, astNode)
            if (treeNode != null) {
                astTree.scrollPathToVisible(TreePath(treeNode.path))
            }
        }
    }

    override fun clearSourceCodeHighlights() {
        sourceCodeEditorPane.highlighter.removeAllHighlights()
    }

    override fun scrollSourceCodeTo(astNode: AstNode?) {
        if (astNode != null && astNode.hasToken()) {
            val visibleLines =
                sourceCodeEditorPane.visibleRect.height / sourceCodeEditorPane.getFontMetrics(sourceCodeEditorPane.font).height
            val line = astNode.token.line + visibleLines / 2
            try {
                sourceCodeEditorPane.scrollRectToVisible(toRectangle(sourceCodeEditorPane.modelToView2D(0)))
                sourceCodeEditorPane.scrollRectToVisible(
                    toRectangle(sourceCodeEditorPane.modelToView2D(lineOffsets.getOffset(line,0)))
                )
            } catch (e: BadLocationException) {
                throw RuntimeException(e)
            }
        }
    }

    override fun disableXPathEvaluateButton() {
        xpathButton.isEnabled = false
    }

    override fun enableXPathEvaluateButton() {
        xpathButton.isEnabled = true
    }

    override val astNodeFollowingCurrentSourceCodeTextCursorPosition: AstNode?
        get() {
            val currentOffset = sourceCodeEditorPane.caretPosition
            return getFollowingAstNode(astTree.model.root as DefaultMutableTreeNode?, currentOffset)
        }

    private fun getFollowingAstNode(treeNode: DefaultMutableTreeNode?, offset: Int): AstNode? {
        var followingAstNode: AstNode? = null
        if (treeNode != null) {
            val enumeration: Enumeration<*> = treeNode.breadthFirstEnumeration()
            var nearestOffsetSoFar = Int.MAX_VALUE
            while (enumeration.hasMoreElements()) {
                val childTreeNode = enumeration.nextElement() as DefaultMutableTreeNode
                if (childTreeNode.userObject is AstNode) {
                    val astNode = childTreeNode.userObject as AstNode
                    if (astNode.hasToken()) {
                        val token = astNode.token
                        val tokenOffset = lineOffsets.getStartOffset(token)
                        if (tokenOffset in offset until nearestOffsetSoFar) {
                            nearestOffsetSoFar = tokenOffset
                            followingAstNode = astNode
                        }
                    }
                }
            }
        }
        return followingAstNode
    }

    override val selectedAstNodes: List<AstNode>
        get() {
            val acc = mutableListOf<AstNode>()
            val selectedPaths = astTree.selectionPaths
            if (selectedPaths != null) {
                for (selectedPath in selectedPaths) {
                    val treeNode = selectedPath.lastPathComponent as DefaultMutableTreeNode
                    val userObject = treeNode.userObject
                    if (userObject is AstNode) {
                        acc.add(userObject)
                    }
                }
            }
            return acc
        }

    override val selectedSymbolOrScopeTree: AstNode?
        get() {
            val treeNode = symbolTree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode
            return when (val userObject = treeNode?.userObject) {
                is Symbol -> userObject.declaration
                is Scope -> userObject.tree
                is AstNode -> userObject
                else -> null
            }
        }

    override fun appendToConsole(message: String?) {
        consoleTextArea.append(message)
    }

    override fun setFocusOnConsoleView() {
        tabbedPane.selectedComponent = consoleScrollPane
    }

    override fun setFocusOnAbstractSyntaxTreeView() {
        tabbedPane.selectedComponent = astTreeScrollPane
    }

    override fun clearConsole() {
        consoleTextArea.text = ""
    }

    override fun addConfigurationProperty(name: String, description: String) {
        val configurationPropertyPanel = ConfigurationPropertyPanel(name, description)
        configurationPropertyPanel.valueTextField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent) {
                presenter.onConfigurationPropertyFocusLost(name)
            }
        })
        configurationPropertiesPanels[name] = configurationPropertyPanel
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        constraints.gridx = 0
        constraints.anchor = GridBagConstraints.NORTH
        configurationInnerPanel.add(configurationPropertyPanel.panel, constraints)
    }

    override fun getConfigurationPropertyValue(name: String): String {
        return configurationPropertiesPanels.getValue(name).valueTextField.text
    }

    override fun setConfigurationPropertyValue(name: String, value: String) {
        configurationPropertiesPanels.getValue(name).valueTextField.text = value
    }

    override fun setConfigurationPropertyErrorMessage(name: String, errorMessage: String) {
        configurationPropertiesPanels.getValue(name).errorMessageLabel.text = errorMessage
    }

    override fun setFocusOnConfigurationPropertyField(name: String) {
        configurationPropertiesPanels.getValue(name).valueTextField.requestFocus()
    }

    override fun setFocusOnConfigurationView() {
        tabbedPane.selectedComponent = configurationScrollPane
    }

    companion object {
        private const val serialVersionUID = 1L
        private val EMPTY_TREE_MODEL: TreeModel = DefaultTreeModel(null)
        private fun getTreeNode(astNode: AstNode): DefaultMutableTreeNode {
            val treeNode = DefaultMutableTreeNode(astNode)
            if (astNode.hasChildren()) {
                for (childAstNode in astNode.children) {
                    treeNode.add(getTreeNode(childAstNode))
                }
            } else if (astNode.hasToken() && astNode.token.hasTrivia()) {
                for (trivia in astNode.token.trivia) {
                    treeNode.add(DefaultMutableTreeNode(trivia))
                }
            }
            return treeNode
        }

        private fun getScopeTreeNode(scope: Scope): DefaultMutableTreeNode {
            val treeNode = DefaultMutableTreeNode(scope)
            if (scope.symbols.isEmpty()) {
                treeNode.add(DefaultMutableTreeNode("<no symbols>"))
            } else {
                val symbolsNode = DefaultMutableTreeNode("Symbols")
                for (symbol in scope.symbols) {
                    val symbolNode = DefaultMutableTreeNode(symbol)
                    if (symbol.usages.isEmpty()) {
                        symbolNode.add(DefaultMutableTreeNode("<no usages>"))
                    } else {
                        val usagesNode = DefaultMutableTreeNode("Usages")
                        for (usage in symbol.usages) {
                            usagesNode.add(DefaultMutableTreeNode(usage))
                        }
                        symbolNode.add(usagesNode)
                    }
                    symbolsNode.add(symbolNode)
                }
                treeNode.add(symbolsNode)
            }
            if (scope.innerScopes.isNotEmpty()) {
                val innerScopesNode = DefaultMutableTreeNode("Scopes")
                for (innerScope in scope.innerScopes) {
                    innerScopesNode.add(getScopeTreeNode(innerScope))
                }
                treeNode.add(innerScopesNode)
            }
            return treeNode
        }
    }

    private fun toRectangle(rectangle2D: Rectangle2D?): Rectangle? {
        return rectangle2D?.let { Rectangle(it.x.toInt(), it.y.toInt(), it.width.toInt(), it.height.toInt()) }
    }
}
