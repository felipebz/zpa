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
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import com.felipebz.flr.toolkit.ConfigurationModel
import com.felipebz.flr.toolkit.ConfigurationProperty
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.*
import java.awt.Point
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class ToolkitPresenterTest {
    @Test
    fun checkInitializedBad() {
        assertThrows<IllegalStateException>("the view must be set before the presenter can be ran") {
            val presenter = ToolkitPresenter(mock(), mock())
            presenter.checkInitialized()
        }
    }

    @Test
    fun checkInitializedGood() {
        val presenter = ToolkitPresenter(mock(), mock())
        presenter.setView(mock())
        presenter.checkInitialized()
    }

    @Test
    fun initUncaughtExceptionsHandler() {
        val view = mock<ToolkitView>()
        val presenter = ToolkitPresenter(mock(), mock())

        presenter.setView(view)
        presenter.initUncaughtExceptionsHandler()
        val uncaughtExceptionHandler = Thread.currentThread().uncaughtExceptionHandler
        assertThat(uncaughtExceptionHandler is ThreadGroup).isFalse()
        val e = mock<Throwable>()
        uncaughtExceptionHandler.uncaughtException(null, e)
        verify(e).printStackTrace(ArgumentMatchers.any(PrintWriter::class.java))
        verify(view).appendToConsole(anyString())
        verify(view).setFocusOnConsoleView()
    }

    @Test
    fun initConfigurationTab() {
        val view = mock<ToolkitView>()
        var presenter = ToolkitPresenter(mock(), mock())
        presenter.setView(view)
        presenter.initConfigurationTab()
        verify(view, never()).addConfigurationProperty(anyString(), anyString())
        verify(view, never()).setConfigurationPropertyValue(anyString(), anyString())
        val property1 = mock<ConfigurationProperty>()
        whenever(property1.name).thenReturn("property1")
        whenever(property1.description).thenReturn("description1")
        whenever(property1.value).thenReturn("default1")
        val property2 = mock<ConfigurationProperty>()
        whenever(property2.name).thenReturn("property2")
        whenever(property2.description).thenReturn("description2")
        whenever(property2.value).thenReturn("default2")
        val configurationModel = mock<ConfigurationModel>()
        whenever(configurationModel.properties).thenReturn(listOf(property1, property2))
        presenter = ToolkitPresenter(configurationModel, mock())
        presenter.setView(view)
        presenter.initConfigurationTab()
        verify(view).addConfigurationProperty("property1", "description1")
        verify(view).setConfigurationPropertyValue("property1", "default1")
        verify(view).addConfigurationProperty("property2", "description2")
        verify(view).setConfigurationPropertyValue("property2", "default2")
    }

    @Test
    fun run() {
        val view = mock<ToolkitView>()
        val presenter = ToolkitPresenter(mock(), mock())
        presenter.setView(view)
        presenter.run("my_mocked_title")
        assertThat(Thread.currentThread().uncaughtExceptionHandler is ThreadGroup).isFalse()
        verify(view).setTitle("my_mocked_title")
        verify(view).displaySourceCode("")
        verify(view).displayAst(null)
        verify(view).displayXml("")
        verify(view).disableXPathEvaluateButton()
        verify(view).run()
    }

    @Test
    fun run_should_call_initConfigurationTab() {
        val view = mock<ToolkitView>()
        var presenter = ToolkitPresenter(mock(), mock())
        presenter.setView(view)
        presenter.run("my_mocked_title")
        verify(view, never()).addConfigurationProperty(anyString(), anyString())
        val configurationModel = mock<ConfigurationModel>()
        whenever(configurationModel.properties).thenReturn(
            listOf(ConfigurationProperty("", "", ""))
        )
        presenter = ToolkitPresenter(configurationModel, mock())
        presenter.setView(view)
        presenter.run("my_mocked_title")
        verify(view).addConfigurationProperty(anyString(), anyString())
    }

    @Test
    fun runFailsWithoutView() {
        assertThrows<IllegalStateException> {
            ToolkitPresenter(mock(), mock()).run("foo")
        }
    }

    @Test
    fun onSourceCodeOpenButtonClick() {
        val view = mock<ToolkitView>()
        val file = File("src/test/resources/parse_error.txt")
        whenever(view.pickFileToParse()).thenReturn(file)
        val model = mock<SourceCodeModel>()
        val astNode = mock<AstNode>()
        whenever(model.sourceCode).thenReturn("my_mocked_highlighted_source_code")
        whenever(model.astNode).thenReturn(astNode)
        whenever(model.xml).thenReturn("my_mocked_xml")
        val presenter = ToolkitPresenter(
            whenever(mock<ConfigurationModel>().charset).thenReturn(StandardCharsets.UTF_8).getMock(),
            model
        )
        presenter.setView(view)
        presenter.onSourceCodeOpenButtonClick()
        verify(view).pickFileToParse()
        verify(view).clearConsole()
        verify(view).displaySourceCode("my_mocked_highlighted_source_code")
        verify(model).setSourceCode(file, StandardCharsets.UTF_8)
        verify(view).displayAst(astNode)
        verify(view).displayXml("my_mocked_xml")
        verify(view).scrollSourceCodeTo(Point(0, 0))
        verify(view).setFocusOnAbstractSyntaxTreeView()
        verify(view).enableXPathEvaluateButton()
    }

    @Test
    fun onSourceCodeOpenButtonClick_with_parse_error_should_clear_console_and_display_code() {
        val view = mock<ToolkitView>()
        val file = File("src/test/resources/parse_error.txt")
        whenever(view.pickFileToParse()).thenReturn(file)
        val model = mock<SourceCodeModel>()
        doThrow(RuntimeException("Parse error")).`when`(model).setSourceCode(any(), any())
        val presenter = ToolkitPresenter(
            whenever(mock<ConfigurationModel>().charset).thenReturn(StandardCharsets.UTF_8).getMock(),
            model
        )
        presenter.setView(view)
        try {
            presenter.onSourceCodeOpenButtonClick()
            throw AssertionError("Expected an exception")
        } catch (e: RuntimeException) {
            verify(view).clearConsole()
            verify(view).displaySourceCode("parse_error.txt")
        }
    }

    @Test
    fun onSourceCodeOpenButtonClick_should_no_operation_when_no_file() {
        val view = mock<ToolkitView>()
        whenever(view.pickFileToParse()).thenReturn(null)
        val model = mock<SourceCodeModel>()
        val presenter = ToolkitPresenter(mock(), model)
        presenter.setView(view)
        presenter.onSourceCodeOpenButtonClick()
        verify(view).pickFileToParse()
        verify(view, never()).clearConsole()
        verify(model, never()).setSourceCode(any(), any())
        verify(view, never()).displaySourceCode(anyString())
        verify(view, never()).displayAst(any())
        verify(view, never()).displayXml(anyString())
        verify(view, never()).scrollSourceCodeTo(any<Point>())
        verify(view, never()).enableXPathEvaluateButton()
    }

    @Test
    fun onSourceCodeParseButtonClick() {
        val view = mock<ToolkitView>()
        whenever(view.sourceCode).thenReturn("my_mocked_source")
        val point = mock<Point>()
        whenever(view.sourceCodeScrollbarPosition).thenReturn(point)
        val model = mock<SourceCodeModel>()
        whenever(model.sourceCode).thenReturn("my_mocked_highlighted_source_code")
        val astNode = mock<AstNode>()
        whenever(model.astNode).thenReturn(astNode)
        whenever(model.xml).thenReturn("my_mocked_xml")
        val presenter = ToolkitPresenter(mock(), model)
        presenter.setView(view)
        presenter.onSourceCodeParseButtonClick()
        verify(view).clearConsole()
        verify(view).sourceCode
        verify(model).setSourceCode("my_mocked_source")
        verify(view).displaySourceCode("my_mocked_highlighted_source_code")
        view.displayAst(astNode)
        view.displayXml("my_mocked_xml")
        view.scrollSourceCodeTo(point)
        verify(view).setFocusOnAbstractSyntaxTreeView()
        verify(view).enableXPathEvaluateButton()
    }

    @Test
    fun onXPathEvaluateButtonClickAstNodeResults() {
        val view = mock<ToolkitView>()
        whenever(view.xPath).thenReturn("//foo")
        val model = mock<SourceCodeModel>()
        val astNode = AstNode(GenericTokenType.IDENTIFIER, "foo", null)
        whenever(model.astNode).thenReturn(astNode)
        val presenter = ToolkitPresenter(mock(), model)
        presenter.setView(view)
        presenter.onXPathEvaluateButtonClick()
        verify(view).clearAstSelections()
        verify(view).clearSourceCodeHighlights()
        verify(view).selectAstNode(astNode)
        verify(view).highlightSourceCode(astNode)
        verify(view).scrollAstTo(astNode)
    }

    @Test
    fun onXPathEvaluateButtonClickScrollToFirstAstNode() {
        val view = mock<ToolkitView>()
        whenever(view.xPath).thenReturn("//foo")
        val model = mock<SourceCodeModel>()
        val astNode = AstNode(GenericTokenType.IDENTIFIER, "foo", null)
        val childAstNode = AstNode(GenericTokenType.IDENTIFIER, "foo", null)
        astNode.addChild(childAstNode)
        whenever(model.astNode).thenReturn(astNode)
        val presenter = ToolkitPresenter(mock(), model)
        presenter.setView(view)
        presenter.onXPathEvaluateButtonClick()
        verify(view).scrollAstTo(astNode)
        verify(view, never()).scrollAstTo(childAstNode)
        verify(view).scrollSourceCodeTo(astNode)
        verify(view, never()).scrollSourceCodeTo(childAstNode)
    }

    @Test
    fun onXPathEvaluateButtonClickStringResult() {
        val view = mock<ToolkitView>()
        whenever(view.xPath).thenReturn("//foo/@tokenValue")
        val model = mock<SourceCodeModel>()
        val token = Token.builder()
            .setType(GenericTokenType.IDENTIFIER)
            .setValueAndOriginalValue("bar")
            .setLine(1)
            .setColumn(1)
            .build()
        val astNode = AstNode(GenericTokenType.IDENTIFIER, "foo", token)
        whenever(model.astNode).thenReturn(astNode)
        val presenter = ToolkitPresenter(mock(), model)
        presenter.setView(view)
        presenter.onXPathEvaluateButtonClick()
        verify(view).clearConsole()
        verify(view).clearAstSelections()
        verify(view).clearSourceCodeHighlights()
        verify(view, never()).selectAstNode(any())
        verify(view, never()).highlightSourceCode(any())
        verify(view).scrollAstTo(null)
        verify(view).scrollSourceCodeTo(null as AstNode?)
        verify(view).setFocusOnAbstractSyntaxTreeView()
    }

    @Test
    fun onSourceCodeKeyTyped() {
        val view = mock<ToolkitView>()
        val presenter = ToolkitPresenter(mock(), mock())
        presenter.setView(view)
        presenter.onSourceCodeKeyTyped()
        verify(view).displayAst(null)
        verify(view).displayXml("")
        verify(view).clearSourceCodeHighlights()
        verify(view).disableXPathEvaluateButton()
    }

    @Test
    fun onSourceCodeTextCursorMoved() {
        val view = mock<ToolkitView>()
        val astNode = mock<AstNode>()
        whenever(view.astNodeFollowingCurrentSourceCodeTextCursorPosition).thenReturn(astNode)
        val presenter = ToolkitPresenter(mock(), mock())
        presenter.setView(view)
        presenter.onSourceCodeTextCursorMoved()
        verify(view).clearAstSelections()
        verify(view).selectAstNode(astNode)
        verify(view).scrollAstTo(astNode)
    }

    @Test
    fun onAstSelectionChanged() {
        val view = mock<ToolkitView>()
        val firstAstNode = mock<AstNode>()
        val secondAstNode = mock<AstNode>()
        whenever(view.selectedAstNodes).thenReturn(listOf(firstAstNode, secondAstNode))
        val presenter = ToolkitPresenter(mock(), mock())
        presenter.setView(view)
        presenter.onAstSelectionChanged()
        verify(view).clearSourceCodeHighlights()
        verify(view).highlightSourceCode(firstAstNode)
        verify(view).highlightSourceCode(secondAstNode)
        verify(view).scrollSourceCodeTo(firstAstNode)
        verify(view, never()).scrollSourceCodeTo(secondAstNode)
    }

    @Test
    fun onConfigurationPropertyFocusLost_when_validation_successes() {
        val view = mock<ToolkitView>()
        val property = mock<ConfigurationProperty>()
        whenever(property.name).thenReturn("name")
        whenever(property.description).thenReturn("description")
        whenever(view.getConfigurationPropertyValue("name")).thenReturn("foo")
        whenever(property.validate("foo")).thenReturn("")
        val configurationModel = mock<ConfigurationModel>()
        whenever(configurationModel.properties).thenReturn(listOf(property))
        val presenter = ToolkitPresenter(configurationModel, mock())
        presenter.setView(view)
        presenter.onConfigurationPropertyFocusLost("name")
        verify(view).setConfigurationPropertyErrorMessage("name", "")
        verify(view, never()).setFocusOnConfigurationPropertyField(anyString())
        verify(view, never()).setFocusOnConfigurationView()
        verify(property).value = "foo"
        verify(configurationModel).setUpdatedFlag()
    }

    @Test
    fun onConfigurationPropertyFocusLost_when_validation_fails() {
        val view = mock<ToolkitView>()
        val property = mock<ConfigurationProperty>()
        whenever(property.name).thenReturn("name")
        whenever(property.description).thenReturn("description")
        whenever(view.getConfigurationPropertyValue("name")).thenReturn("foo")
        whenever(property.validate("foo")).thenReturn("The value foo is forbidden!")
        val configurationModel = mock<ConfigurationModel>()
        whenever(configurationModel.properties).thenReturn(listOf(property))
        val presenter = ToolkitPresenter(configurationModel, mock())
        presenter.setView(view)
        presenter.onConfigurationPropertyFocusLost("name")
        verify(view).setConfigurationPropertyErrorMessage("name", "The value foo is forbidden!")
        verify(view).setFocusOnConfigurationPropertyField("name")
        verify(view).setFocusOnConfigurationView()
        verify(property, never()).value = "foo"
        verify(configurationModel, never()).setUpdatedFlag()
    }

    @Test
    fun onConfigurationPropertyFocusLost_with_invalid_name() {
        val view = mock<ToolkitView>()
        val presenter = ToolkitPresenter(mock(), mock())
        presenter.setView(view)
        assertThrows<IllegalArgumentException>("No such configuration property: name") {
            presenter.onConfigurationPropertyFocusLost("name")
        }
    }
}
