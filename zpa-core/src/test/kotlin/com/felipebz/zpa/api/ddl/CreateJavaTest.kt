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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateJavaTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_JAVA)
    }

    @Test
    fun matchesJavaSourceAtEndOfInput() {
        assertThat(p).matches(
            """
            create java source named "Example" as
            public class Example {
                public int divide(int left, int right) {
                    return left / right;
                }
            }
            """.trimIndent())
    }

    @Test
    fun matchesJavaSourceWithEnvelopeOptions() {
        assertThat(p).matches(
            """
            create or replace and resolve noforce java if not exists source named app.Example
                sharing = metadata
                authid current_user
                resolver (("(*", "PUBLIC"))
                as
            public class Example {}
            """.trimIndent())
    }

    @Test
    fun matchesJavaSourceLoadedFromBfile() {
        assertThat(p).matches("create java source named Example using bfile (java_dir, 'Example.java')")
    }

    @Test
    fun matchesUnquotedJavaSourceName() {
        assertThat(p).matches("create java source named Example as public class Example {}")
    }

    @Test
    fun matchesJavaClassAndResource() {
        assertThat(p).matches("create java class using bfile (java_dir, 'Agent.class');")
        assertThat(p).matches("create java class schema app using bfile (java_dir, 'Agent.class')")
        assertThat(p).matches("create java class schema \"AppSchema\" using bfile (java_dir, 'Agent.class')")
        assertThat(p).matches("create java resource named appText using bfile (java_dir, 'textBundle.dat');")
    }

    @Test
    fun matchesStructuredResolverEntries() {
        assertThat(p).matches(
            "create java source named Example resolver ((* HR)) as public class Example {}")
        assertThat(p).matches(
            "create java source named Example resolver ((*, HR)) as public class Example {}")
        assertThat(p).matches(
            "create java source named Example resolver ((\"java/lang/String\" HR)) as public class Example {}")
        assertThat(p).matches(
            "create java source named Example resolver ((\"java/lang/String\", HR)) as public class Example {}")
        assertThat(p).matches(
            "create java source named Example resolver ((\"java/lang/*\" HR)) as public class Example {}")
        assertThat(p).matches(
            "create java source named Example resolver ((\"java/lang/*\", HR)) as public class Example {}")
        assertThat(p).matches(
            "create java source named Example resolver ((* PUBLIC)(* -)) as public class Example {}")

    }
    @Test
    fun matchesUsingSubqueryWithDivision() {
        assertThat(p).matches(
            """
            create java resource named appText using blob (
                select blob_content
                from files
                where id = 1 and 4 / 2 = 2
            )
            """.trimIndent())
    }

    @Test
    fun matchesOpaqueJavaBodyWithJavaSyntax() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches(
            """
            create java source named "Opaque" as
            // Java line comment
            /* Java block comment */
            @Deprecated
            class Opaque {
                List<String> values;
                String[] names;
                int divide(int left, int right) {
                    return left / right;
                }
            }
            /
            select 1 from dual;
            """.trimIndent())
    }

    @Test
    fun rejectsMalformedResolverEntries() {
        assertThat(p).notMatches("create java source named Example resolver (garbage) as public class Example {}")
        assertThat(p).notMatches("create java source named Example resolver (* HR) as public class Example {}")
        assertThat(p).notMatches("create java source named Example resolver ((java/lang/String HR)) as public class Example {}")
    }

    @Test
    fun rejectsMalformedUsingClauses() {
        assertThat(p).notMatches("create java resource named appText using blob")
        assertThat(p).notMatches("create java resource named appText using bfile (java_dir, Agent.class)")
        assertThat(p).notMatches("create java resource named appText using bfile (schema.java_dir, 'textBundle.dat')")
    }

    @Test
    fun matchesJavaKeyLoading() {
        assertThat(p).matches("create java resource named appText using 'textBundle.dat'")
    }

    @Test
    fun rejectsQualifiedClassSchema() {
        assertThat(p).notMatches("create java class schema foo.bar using bfile (java_dir, 'Agent.class')")
    }

    @Test
    fun matchesStandaloneSlashAndFollowingStatement() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        assertThat(p).matches(
            """
            create java source named "Example" as
            public class Example {
                public static String text() {
                    return "text // and /";
                }
            }
            /
            select * from dual;
            """.trimIndent())
    }

    @Test
    fun rejectsMissingJavaSourceName() {
        assertThat(p).notMatches("create java source as public class Example {}")
    }

    @Test
    fun rejectsNamedJavaClass() {
        assertThat(p).notMatches("create java class named Example using bfile (java_dir, 'Agent.class')")
    }

    @Test
    fun rejectsJavaAsClauses() {
        assertThat(p).notMatches("create java resource named appText as resource text")
        assertThat(p).notMatches("create java class as class text")
    }
}
