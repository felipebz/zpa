//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class XmlTableExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesSimpleXmlTable() {
        assertThat(p).matches("xmltable('/foo' passing bar)")
        assertThat(p).matches("xmltable('/foo' passing bar.baz)")
    }

    @Test
    fun matchesXmlTableWithNamespaces() {
        assertThat(p).matches("xmltable(xmlnamespaces ('foo' as bar, default 'foo2'), '/foo/bar:foo' passing bar)")
    }

    @Test
    fun matchesXmlTableReturningSequenceByRef() {
        assertThat(p).matches("xmltable('/foo' passing bar returning sequence by ref)")
    }

    @Test
    fun matchesXmlTableWithColumns() {
        assertThat(p).matches("xmltable('/foo' passing bar columns \"foo\" varchar2(1) path 'bar' default 'a', \"foo2\" varchar2(1) path 'bar2', \"foo3\" for ordinality)")
    }


    @Test
    fun matchesXmlTableWithPathExpression() {
        assertThat(p).matches("xmltable(l_path passing doc columns c1 varchar2(10) path 'a')")
    }

    @Test
    fun matchesXmlTableColumnWithoutDatatype() {
        assertThat(p).matches("xmltable('/a' passing doc columns c1 path 'text()')")
    }

    @Test
    fun matchesXmlTablePassingSeveralExpressions() {
        assertThat(p).matches("xmltable('/a' passing doc, to_number(x) + 1 as \"i\" columns c1 varchar2(10) path 'a')")
    }

    @Test
    fun doesNotMatchXmlTableWithoutXQuery() {
        assertThat(p).notMatches("xmltable(passing doc columns c1 path 'a')")
    }
}
