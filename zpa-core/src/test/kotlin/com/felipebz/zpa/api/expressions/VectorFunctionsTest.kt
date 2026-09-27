/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.grammar.GrammarRuleKey
import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.AggregateSqlFunctionsGrammar
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VectorFunctionsTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    private fun assertMatches(vararg sources: String) {
        for (source in sources) {
            assertThat(p).describedAs(source).matches(source)
        }
    }

    private fun assertNotMatches(vararg sources: String) {
        for (source in sources) {
            assertThat(p).describedAs(source).notMatches(source)
        }
    }

    private fun assertParsesAs(source: String, ruleKey: GrammarRuleKey) {
        assertThatAst(p.parse(source).getDescendants(ruleKey)).describedAs(source).hasSize(1)
    }

    private fun assertGenericCall(source: String) {
        val node = p.parse(source)
        assertThatAst(node.getDescendants(SingleRowSqlFunctionsGrammar.SINGLE_ROW_SQL_FUNCTION)).describedAs(source).isEmpty()
        assertThatAst(node.getDescendants(AggregateSqlFunctionsGrammar.AGGREGATE_SQL_FUNCTION)).describedAs(source).isEmpty()
        assertThatAst(node.getDescendants(PlSqlGrammar.METHOD_CALL)).describedAs(source).isNotEmpty
    }

    @Test
    fun parsesVectorSerialization() {
        assertParsesAs("from_vector(to_vector('[1.1, 2.2, 3.3]', 3, float32) returning varchar2(1000))",
            SingleRowSqlFunctionsGrammar.FROM_VECTOR_EXPRESSION)
        assertParsesAs("vector_serialize(vector('[1.1, 2.2, 3.3]', 3, float32) returning clob)",
            SingleRowSqlFunctionsGrammar.VECTOR_SERIALIZE_EXPRESSION)
        for (function in listOf("from_vector", "vector_serialize")) {
            assertMatches(
                "$function(v)",
                "$function(v returning varchar2)",
                "$function(v returning varchar2(100 byte))",
                "$function(v returning varchar2(100 char) format dense)",
                "$function(v returning varchar(100))",
                "$function(to_vector('[5,[2,4],[1.0,2.0]]', 5, float64, sparse) returning clob format sparse)",
                "$function(v returning blob format sparse)",
                "$function(v format dense)"
            )
            assertNotMatches(
                // ORA-00907
                "$function(v format dense returning clob)",
                "$function(v returning varchar2(10 + 10))",
                "$function(v returning clob(100))",
                "$function(v returning varchar2 byte)",
                "$function(v returning clob format dense format sparse)",
                "$function(v returning clob returning clob)",
                // ORA-51809
                "$function(v returning number)",
                "$function(v returning nclob)",
                "$function(v returning char(10))",
                "$function(v returning)",
                // ORA-51818
                "$function(v returning clob format json)",
                "$function(v returning clob format)",
                // ORA-00910
                "$function(v returning varchar2())"
            )
        }
    }

    @Test
    fun parsesVectorEmbedding() {
        assertParsesAs("vector_embedding(model using 'hello' as data)", AggregateSqlFunctionsGrammar.VECTOR_EMBEDDING_EXPRESSION)
        assertMatches(
            "to_vector(vector_embedding(model using 'hello' as data))",
            "vector_embedding(sch.model using 'hello' data)",
            "vector_embedding(model using *)",
            "vector_embedding(model using t.*)",
            "vector_embedding(model using 1 as a, 2 as b)",
            // ORA-00998 (a literal needs an alias) is not a syntax error.
            "vector_embedding(model using 'hello')"
        )
        assertNotMatches(
            // ORA-02012
            "vector_embedding(a.b.model using 'hello')",
            // ORA-00907
            "vector_embedding(model using 'hello' as data foo bar)",
            // ORA-00936
            "vector_embedding(model using 1 as a,)",
            // ORA-00923
            "vector_embedding(model using 'a' as x) over ()",
            "vector_embedding(model using 'a' as x) filter (where 1 = 1)"
        )
    }

    @Test
    fun keepsNonMatchingCallsGeneric() {
        assertGenericCall("from_vector(a, b)")
        assertGenericCall("vector_serialize(a, b)")
        assertGenericCall("vector_embedding(model)")
        assertGenericCall("vector_embedding('m', 1)")
        assertGenericCall("vector_chunks('text')")
    }

    @Test
    fun parsesVectorChunksRowSource() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
        val node = p.parse("select * from vector_chunks(:txt by words max 10)")
        assertThatAst(node.getDescendants(DmlGrammar.VECTOR_CHUNKS_TABLE)).hasSize(1)
        assertThatAst(node.getDescendants(PlSqlGrammar.METHOD_CALL)).isEmpty()
        assertMatches(
            "select * from vector_chunks('text')",
            """
            select d.id id, c.chunk_offset pos, c.chunk_length siz, c.chunk_text txt
            from documentation_tab d, vector_chunks(d.text
                                          by words
                                          max 200
                                          overlap 10
                                          split by recursively
                                          language american
                                          normalize all) c
            """.trimIndent(),
            "select * from vector_chunks(t.a || 'x' by chars max :m:i overlap :o split newline extended)",
            "select * from vector_chunks('x' by characters split by custom (q'[;]', 'a') language \"AMERICAN\")",
            "select * from vector_chunks('x' by vocabulary sch.vocab normalize (whitespace, punctuation, widechar))",
            "select * from vector_chunks((select 'a b' from dual) split by none normalize none)",
            "select * from t cross apply vector_chunks(t.x) c where c.chunk_offset > 1",
            "select * from t left join vector_chunks(t.x) c on 1 = 1"
        )
        assertNotMatches(
            // ORA-02000: options appear once and in the documented order; one argument only.
            "select * from vector_chunks('x' max 10 by words)",
            "select * from vector_chunks('x' overlap 5 max 50)",
            "select * from vector_chunks('x' language american split by newline)",
            "select * from vector_chunks('x' normalize all language american)",
            "select * from vector_chunks('x' extended by words)",
            "select * from vector_chunks('x' max 10 max 20)",
            "select * from vector_chunks('x' extended extended)",
            "select * from vector_chunks('x', 1)",
            "select * from vector_chunks('x' max 50 + 50)",
            "select * from vector_chunks('x' split custom 'a')",
            // ORA-30583 / ORA-30584 / ORA-30586 / ORA-30587 / ORA-30589
            "select * from vector_chunks('x' by chunker ch)",
            "select * from vector_chunks('x' by lines)",
            "select * from vector_chunks('x' by)",
            "select * from vector_chunks('x' max x)",
            "select * from vector_chunks('x' max (50))",
            "select * from vector_chunks('x' split by foo)",
            "select * from vector_chunks('x' split by custom ())",
            "select * from vector_chunks('x' normalize whitespace)",
            "select * from vector_chunks('x' normalize ())",
            // ORA-03050
            "select * from vector_chunks('x' language 'american')",
            // ORA-00931
            "select * from vector_chunks('x' by vocabulary)",
            // ORA-00936
            "select * from vector_chunks()"
        )
    }
}
