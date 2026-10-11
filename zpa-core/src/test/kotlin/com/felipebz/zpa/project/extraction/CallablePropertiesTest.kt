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
package com.felipebz.zpa.project.extraction

import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectDeclarationExtractor
import com.felipebz.zpa.project.StandaloneFunctionDeclaration
import com.felipebz.zpa.project.qualifiedName
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** Strict property scanning, called on hand-lexed property sequences. */
class CallablePropertiesTest {
    private class Fixture(text: String) {
        val stream = TokenStream(FileId("props.sql"), PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text))
        val properties = CallableProperties(stream)
        val limit = stream.tokens.size - 1 // before EOF
        fun scan(function: Boolean, limit: Int = this.limit) = properties.skipPropertiesStrict(0, limit, function)
        fun at(value: String, n: Int = 0): Int = stream.tokens.indices.filter { stream.valueAt(it) == value }[n]
    }

    @Test
    fun ordinaryPropertiesStopAtTheFirstUnknownTokenWithTheOriginalNextIndex() {
        Fixture("IS BEGIN NULL; END;").scan(function = false)!!.let {
            assertThat(it.next).isEqualTo(0)
            assertThat(it.deterministic).isFalse()
            assertThat(it.terminal).isFalse()
        }
        Fixture("DETERMINISTIC SHARD_ENABLE IS BEGIN NULL; END;").scan(function = true)!!.let {
            assertThat(it.next).isEqualTo(2)
            assertThat(it.deterministic).isTrue()
            assertThat(it.terminal).isFalse()
        }
        Fixture("SHARD_ENABLE AS").scan(function = false)!!.let { assertThat(it.next).isEqualTo(1); assertThat(it.deterministic).isFalse() }
        Fixture("PIPELINED ROW POLYMORPHIC IS").scan(function = true)!!.let { assertThat(it.next).isEqualTo(3) }
        Fixture("PIPELINED TABLE POLYMORPHIC DETERMINISTIC IS").scan(function = true)!!.let {
            assertThat(it.next).isEqualTo(4)
            assertThat(it.deterministic).isTrue()
        }
        assertThat(Fixture("PIPELINED IS").scan(function = true)!!.next).isEqualTo(1)
    }

    @Test
    fun terminalUsingFormsAreFunctionOnlyAndReturnTheSemicolonIndex() {
        val pipelined = Fixture("PIPELINED USING s.impl;")
        pipelined.scan(function = true)!!.let {
            assertThat(it.terminal).isTrue()
            assertThat(it.next).isEqualTo(pipelined.at(";"))
        }
        val aggregate = Fixture("DETERMINISTIC AGGREGATE USING impl;")
        aggregate.scan(function = true)!!.let {
            assertThat(it.terminal).isTrue()
            assertThat(it.deterministic).isTrue()
            assertThat(it.next).isEqualTo(aggregate.at(";"))
        }
        assertThat(Fixture("PIPELINED USING impl;").scan(function = false)).isNull()
        assertThat(Fixture("AGGREGATE USING impl;").scan(function = false)).isNull()
        assertThat(Fixture("AGGREGATE impl;").scan(function = true)).isNull()
        assertThat(Fixture("PIPELINED USING impl").scan(function = true)).describedAs("no semicolon").isNull()
    }

    @Test
    fun resultCacheAuthidCollationAndAccessibleByKeepTheirMandatoryStructure() {
        assertThat(Fixture("RESULT_CACHE IS").scan(false)!!.next).isEqualTo(1)
        val relies = Fixture("RESULT_CACHE RELIES_ON (a, s.b) IS")
        assertThat(relies.scan(false)!!.next).isEqualTo(relies.at("IS"))
        assertThat(Fixture("RESULT_CACHE RELIES_ON a IS").scan(false)).isNull()
        assertThat(Fixture("AUTHID CURRENT_USER IS").scan(false)!!.next).isEqualTo(2)
        assertThat(Fixture("AUTHID DEFINER IS").scan(false)!!.next).isEqualTo(2)
        assertThat(Fixture("AUTHID OTHER IS").scan(false)).isNull()
        assertThat(Fixture("DEFAULT COLLATION USING_NLS_COMP IS").scan(false)!!.next).isEqualTo(3)
        assertThat(Fixture("DEFAULT COLLATION OTHER IS").scan(false)).isNull()
        assertThat(Fixture("DEFAULT x IS").scan(false)).isNull()
        val accessible = Fixture("ACCESSIBLE BY (FUNCTION s.f, PACKAGE p, q) IS")
        assertThat(accessible.scan(false)!!.next).isEqualTo(accessible.at("IS"))
        assertThat(Fixture("ACCESSIBLE BY () IS").scan(false)).isNull()
        assertThat(Fixture("ACCESSIBLE x IS").scan(false)).isNull()
        assertThat(Fixture("ACCESSIBLE BY (a.b.c) IS").scan(false)).describedAs("three-segment accessor").isNull()
    }

    @Test
    fun sqlMacroParallelEnableAndStreamingClauses() {
        assertThat(Fixture("SQL_MACRO IS").scan(true)!!.next).isEqualTo(1)
        assertThat(Fixture("SQL_MACRO(TABLE) IS").scan(true)!!.next).isEqualTo(4)
        assertThat(Fixture("SQL_MACRO(SCALAR) IS").scan(true)!!.next).isEqualTo(4)
        assertThat(Fixture("SQL_MACRO(TYPE => TABLE) IS").scan(true)!!.next).isEqualTo(6)
        assertThat(Fixture("SQL_MACRO(OTHER) IS").scan(true)).isNull()
        assertThat(Fixture("PARALLEL_ENABLE IS").scan(true)!!.next).isEqualTo(1)
        val any = Fixture("PARALLEL_ENABLE (PARTITION a BY ANY) IS")
        assertThat(any.scan(true)!!.next).isEqualTo(any.at("IS"))
        val hash = Fixture("PARALLEL_ENABLE (PARTITION a BY HASH (b, c) ORDER d BY (e)) IS")
        assertThat(hash.scan(true)!!.next).isEqualTo(hash.at("IS"))
        val cluster = Fixture("CLUSTER d BY (e, f) IS")
        assertThat(cluster.scan(false)!!.next).isEqualTo(cluster.at("IS"))
        assertThat(Fixture("PARALLEL_ENABLE (PARTITION a BY) IS").scan(true)).isNull()
        assertThat(Fixture("ORDER d BY e IS").scan(false)).isNull()
    }

    @Test
    fun limitsAreRespectedAndMalformedKnownPropertiesYieldNull() {
        val f = Fixture("DETERMINISTIC IS")
        assertThat(f.scan(true, limit = 2)!!.next).isEqualTo(1)
        // The scan only ends successfully on a token it does not know; running into the limit is a failure.
        assertThat(f.scan(true, limit = 1)).isNull()
        assertThat(f.scan(true, limit = 0)).isNull()
        assertThat(Fixture("RESULT_CACHE RELIES_ON (a").scan(false)).isNull()
        assertThat(Fixture("ACCESSIBLE BY (a").scan(false)).isNull()
    }

    @Test
    fun completeStandaloneUnitsStillExtractThroughTheFacade() {
        val fileId = FileId("units.sql")
        val facts = ProjectDeclarationExtractor().extract(
            fileId,
            "CREATE FUNCTION f(a NUMBER) RETURN NUMBER DETERMINISTIC RESULT_CACHE AUTHID DEFINER IS BEGIN RETURN a; END;\n" +
                "CREATE FUNCTION pf RETURN NUMBER PIPELINED USING impl;\n" +
                "CREATE PROCEDURE p AUTHID CURRENT_USER IS BEGIN NULL; END;\n" +
                "CREATE PROCEDURE bad PIPELINED USING impl;\n" +
                "CREATE FUNCTION after RETURN NUMBER IS BEGIN RETURN 1; END;"
        )
        assertThat(facts.map { it.qualifiedName().toString() }).containsExactly("f", "pf", "p", "after")
        assertThat((facts[0] as StandaloneFunctionDeclaration).deterministic).isTrue()
        assertThat((facts[1] as StandaloneFunctionDeclaration).deterministic).isFalse()
    }
}
