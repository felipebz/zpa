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
import com.felipebz.zpa.project.qualifiedName
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/** `LANGUAGE C`, `EXTERNAL` and `MLE` boundary recognition on hand-lexed call specifications. */
class CallSpecificationScannerTest {
    private class Fixture(text: String) {
        val stream = TokenStream(FileId("calls.sql"), PlSqlLexer.create(PlSqlConfiguration(StandardCharsets.UTF_8, true)).lex(text))
        val scanner = CallSpecificationScanner(stream, DatatypeShape(stream))
        val limit = stream.tokens.size - 1
        fun semicolon(): Int = stream.tokens.indices.last { stream.valueAt(it) == ";" }
        fun c(limit: Int = this.limit) = scanner.cSpecificationEnd(0, limit)
        fun mle(limit: Int = this.limit) = scanner.mleSpecificationEnd(1, limit)
    }

    private fun accepted(text: String) = Fixture(text).let { assertThat(it.c()).describedAs(text).isEqualTo(it.semicolon()) }
    private fun rejected(text: String) = assertThat(Fixture(text).c()).describedAs(text).isNull()

    @Test
    fun languageCAndExternalWithTheirOptionalClauses() {
        accepted("LANGUAGE C LIBRARY lib;")
        accepted("LANGUAGE c NAME \"n\" LIBRARY lib NAME m;")
        accepted("EXTERNAL LIBRARY lib;")
        accepted("EXTERNAL NAME x LIBRARY lib AGENT IN (a, b) WITH CONTEXT PARAMETERS (CONTEXT, a INT, RETURN INT);")
        accepted("LANGUAGE C LIBRARY lib AGENT IN (a);")
        accepted("LANGUAGE C LIBRARY lib WITH CONTEXT;")
        rejected("LANGUAGE \"C\" LIBRARY lib;")
        rejected("LANGUAGE C;")
        rejected("LANGUAGE C NAME x;")
        rejected("LANGUAGE JAVA LIBRARY lib;")
        rejected("EXTERNAL LIBRARY lib AGENT (a);")
        rejected("EXTERNAL LIBRARY lib AGENT IN ();")
        rejected("EXTERNAL LIBRARY lib WITH;")
        rejected("LANGUAGE C LIBRARY lib")
        assertThat(Fixture("LANGUAGE C LIBRARY lib;").c(limit = 4)).describedAs("semicolon at the limit").isNull()
    }

    @Test
    fun externalParametersFollowTheOrderedAlternatives() {
        accepted("EXTERNAL LIBRARY l PARAMETERS (CONTEXT);")
        accepted("EXTERNAL LIBRARY l PARAMETERS (CONTEXT, a INT);")
        accepted("EXTERNAL LIBRARY l PARAMETERS (SELF TDO, a INT);")
        accepted("EXTERNAL LIBRARY l PARAMETERS (SELF LENGTH, a INT);")
        accepted("EXTERNAL LIBRARY l PARAMETERS (SELF, a INT);") // SELF as an ordinary name
        accepted("EXTERNAL LIBRARY l PARAMETERS (CONTEXT x);") // CONTEXT as an ordinary name
        accepted("EXTERNAL LIBRARY l PARAMETERS (a INDICATOR STRUCT, b INDICATOR TDO, c INDICATOR, d LENGTH, e DURATION, f MAXLEN, g CHARSETID, h CHARSETFORM);")
        accepted("EXTERNAL LIBRARY l PARAMETERS (a BY REFERENCE, b LENGTH BY REFERENCE STRING, RETURN INDICATOR);")
        accepted("EXTERNAL LIBRARY l PARAMETERS (a t.x%TYPE, b NUMBER(10, 2), RETURN INT);")
        rejected("EXTERNAL LIBRARY l PARAMETERS (a BY x);")
        rejected("EXTERNAL LIBRARY l PARAMETERS ();")
        rejected("EXTERNAL LIBRARY l PARAMETERS (a INT, );")
        rejected("EXTERNAL LIBRARY l PARAMETERS (, a);")
        rejected("EXTERNAL LIBRARY l PARAMETERS (a NUMBER b);")
        rejected("EXTERNAL LIBRARY l PARAMETERS (a f(1]);")
        rejected("EXTERNAL LIBRARY l PARAMETERS (a INT;")
        rejected("EXTERNAL LIBRARY l PARAMETERS (1);")
    }

    @Test
    fun mleInlineAndModuleSignatureForms() {
        // The lexer only produces the inline-source token after AS or IS, so these start two tokens in.
        val inline = Fixture("AS MLE LANGUAGE JAVASCRIPT {{ BEGIN END; CREATE ; }};")
        assertThat(inline.scanner.mleSpecificationEnd(2, inline.limit)).isEqualTo(inline.semicolon())
        val pure = Fixture("AS MLE LANGUAGE js.sub PURE {{ x }};")
        assertThat(pure.scanner.mleSpecificationEnd(2, pure.limit)).isEqualTo(pure.semicolon())
        assertThat(Fixture("MLE LANGUAGE JAVASCRIPT {{ x }};").mle()).describedAs("no AS/IS before: no inline token").isNull()
        val module = Fixture("MLE MODULE m SIGNATURE 'f(number)';")
        assertThat(module.mle()).isEqualTo(module.semicolon())
        val env = Fixture("MLE MODULE s.m ENV s.e SIGNATURE 'f()';")
        assertThat(env.mle()).isEqualTo(env.semicolon())
        assertThat(Fixture("MLE MODULE m SIGNATURE x;").mle()).isNull()
        assertThat(Fixture("MLE MODULE m ENV e;").mle()).isNull()
        assertThat(Fixture("MLE MODULE SIGNATURE 's';").mle()).isNull()
        assertThat(Fixture("MLE LANGUAGE;").mle()).isNull()
        assertThat(Fixture("MLE LANGUAGE js PURE;").mle()).isNull()
        assertThat(Fixture("MLE OTHER x;").mle()).isNull()
    }

    @Test
    fun completeStandaloneUnitsWithCallSpecificationsStillExtractThroughTheFacade() {
        val facts = ProjectDeclarationExtractor().extract(
            FileId("units.sql"),
            "CREATE PROCEDURE c(a NUMBER) AS LANGUAGE C LIBRARY lib PARAMETERS (a INT);\n" +
                "CREATE FUNCTION e RETURN NUMBER AS EXTERNAL LIBRARY lib WITH CONTEXT;\n" +
                "CREATE FUNCTION m RETURN NUMBER AS MLE MODULE mod SIGNATURE 'f()';\n" +
                "CREATE PROCEDURE quoted AS LANGUAGE \"C\" LIBRARY lib;\n" +
                "CREATE PROCEDURE ok IS BEGIN NULL; END;"
        )
        assertThat(facts.map { it.qualifiedName().toString() }).containsExactly("c", "e", "m", "ok")
    }
}
