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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.RuleTest
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class CreateSequenceTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_SEQUENCE)
    }

    @Test
    fun matchesSimpleCreateSequence() {
        assertThat(p).matches("create sequence seq_name;")
    }

    @Test
    fun matchesSchemaQualifiedCreateSequence() {
        assertThat(p).matches("create sequence schema.seq_name;")
    }

    @Test
    fun matchesCreateSequenceWithoutSemicolon() {
        assertThat(p).matches("create sequence seq_name")
    }

    @Test
    fun matchesCreateSequenceStart() {
        assertThat(p).matches("create sequence seq_name start with 1;")
    }

    @Test
    fun matchesCreateSequenceIncrement() {
        assertThat(p).matches("create sequence seq_name increment by 1;")
    }

    @Test
    fun matchesCreateSequenceStartIncrement() {
        assertThat(p).matches("create sequence seq_name start with 1 increment by 1;")
    }

    @Test
    fun matchesCreateSequenceCache() {
        assertThat(p).matches("create sequence seq_name cache 10;")
    }

    @Test
    fun matchesCreateSequenceNoCache() {
        assertThat(p).matches("create sequence seq_name nocache;")
    }

    @Test
    fun matchesCreateSequenceOptionsInOracleExampleOrder() {
        assertThat(p).matches("create sequence seq_name start with 1 increment by 1 nocache nocycle;")
    }

    @Test
    fun matchesCreateSequenceOptionsInDifferentOrder() {
        assertThat(p).matches("create sequence seq_name cache 20 minvalue 1 maxvalue 999 cycle;")
    }

    @Test
    fun matchesCreateSequenceIncrementBeforeStart() {
        assertThat(p).matches("create sequence seq_name increment by 1 start with 1;")
    }

    @Test
    fun acceptsRepeatedOptionsAtSyntaxLevel() {
        assertThat(p).matches("create sequence seq_name start with 1 start with 2;")
    }

    @Test
    fun matchesCreateSequenceOrderOptionsWithoutCache() {
        assertThat(p).matches("create sequence seq_name order;")
        assertThat(p).matches("create sequence seq_name noorder;")
    }

    @Test
    fun matchesCreateSequenceSharingOptions() {
        assertThat(p).matches("create sequence seq_name sharing = metadata;")
        assertThat(p).matches("create sequence seq_name sharing = data;")
        assertThat(p).matches("create sequence seq_name sharing = none;")
    }

    @Test
    fun matchesCreateSequenceUnboundedOptions() {
        assertThat(p).matches("create sequence seq_name nomaxvalue;")
        assertThat(p).matches("create sequence seq_name nominvalue;")
    }

    @Test
    fun matchesCreateSequenceKeepOptions() {
        assertThat(p).matches("create sequence seq_name keep;")
        assertThat(p).matches("create sequence seq_name nokeep;")
    }

    @Test
    fun matchesCreateSequenceScaleOptions() {
        assertThat(p).matches("create sequence seq_name scale extend;")
        assertThat(p).matches("create sequence seq_name scale noextend;")
        assertThat(p).matches("create sequence seq_name noscale;")
    }

    @Test
    fun matchesCreateSequenceSessionOptions() {
        assertThat(p).matches("create sequence seq_name session;")
        assertThat(p).matches("create sequence seq_name global;")
    }

    @Test
    fun matchesCreateSequenceSignedIntegerOptions() {
        assertThat(p).matches("create sequence seq_name increment by -1;")
        assertThat(p).matches("create sequence seq_name start with +1;")
        assertThat(p).matches("create sequence seq_name cache +2;")
    }

    @Test
    fun matchesSequenceNamesUsingNonReservedSequenceKeywords() {
        listOf("nomaxvalue", "nominvalue", "nokeep", "noextend", "noscale", "scale")
            .forEach { sequenceName ->
                assertThat(p).matches("create sequence $sequenceName;")
            }
    }

    @Test
    fun rejectsNonIntegerSequenceOptions() {
        assertThat(p).notMatches("create sequence seq_name start with 1.5;")
        assertThat(p).notMatches("create sequence seq_name increment by 1e2;")
        assertThat(p).notMatches("create sequence seq_name maxvalue .5;")
    }

    @Test
    fun rejectsIncompleteSequenceOptions() {
        assertThat(p).notMatches("create sequence seq_name start;")
        assertThat(p).notMatches("create sequence seq_name start with;")
        assertThat(p).notMatches("create sequence seq_name increment;")
        assertThat(p).notMatches("create sequence seq_name increment by;")
        assertThat(p).notMatches("create sequence seq_name cache;")
        assertThat(p).notMatches("create sequence seq_name sharing;")
        assertThat(p).notMatches("create sequence seq_name sharing =;")
        assertThat(p).notMatches("create sequence seq_name start with 1 sharing = metadata;")
        assertThat(p).notMatches("create sequence seq_name scale;")
        assertThat(p).notMatches("create sequence seq_name cache 20 unexpected;")
    }

    @Test
    fun preservesCreateSequenceAstShape() {
        val node = p.parse("create sequence schema.seq_name start with 1 cache 20 order;")

        assertThatAst(node.type).isEqualTo(DdlGrammar.CREATE_SEQUENCE)
        assertThatAst(node.children.map { it.type }).containsExactly(
            PlSqlKeyword.CREATE,
            PlSqlKeyword.SEQUENCE,
            PlSqlGrammar.UNIT_NAME,
            PlSqlKeyword.START,
            PlSqlKeyword.WITH,
            PlSqlGrammar.NUMERIC_LITERAL,
            PlSqlKeyword.CACHE,
            PlSqlGrammar.NUMERIC_LITERAL,
            PlSqlKeyword.ORDER,
            PlSqlPunctuator.SEMICOLON)
    }
}
