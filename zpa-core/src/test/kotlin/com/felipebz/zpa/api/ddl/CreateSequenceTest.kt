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
        // Oracle 26 parses SCALE without EXTEND/NOEXTEND.
        assertThat(p).matches("create sequence seq_name scale;")
        assertThat(p).matches("create sequence seq_name scale nocache;")
    }

    @Test
    fun matchesCreateSequenceIfNotExists() {
        assertThat(p).matches("create sequence if not exists email_seq;")
        assertThat(p).matches("create sequence if not exists app.seq start with 1 cache 20;")
    }

    @Test
    fun matchesShardOptions() {
        listOf(
            "create sequence s shard;", "create sequence s shard extend;", "create sequence s shard noextend;",
            "create sequence s noshard;", "create sequence s increment by 1 shard;",
            "create sequence s increment by 1 shard extend;", "create sequence s increment by 1 noshard;",
            "create sequence s shard scale;", "create sequence s scale shard;",
            "create sequence s shard extend scale;", "create sequence s scale shard extend;", "create sequence s shard scale noextend;",
            "create sequence s scale extend shard;", "create sequence s shard noextend scale;", "create sequence s scale shard noextend",
            "create sequence s start with 100 increment by 5 maxvalue 10000 minvalue 100 nocycle cache 100 order keep shard extend global;",
            "create sequence s global shard extend scale keep order cache 20 cycle maxvalue 999999 minvalue 1 increment by 1 start with 1;",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "create sequence s shard foo;", "create sequence s noshard extend;", "create sequence s noshard noextend;",
            "create sequence s shard extend 1;",
            "create sequence s shard extend scale extend;", "create sequence s shard noextend scale noextend;",
            "create sequence s shard extend scale noextend;", "create sequence s shard noextend scale extend;",
            "create sequence s scale extend shard extend;", "create sequence s scale noextend shard noextend;",
            "create sequence s scale extend shard noextend;", "create sequence s scale noextend shard extend;",
            "create sequence s shard extend cache 20 scale extend;",
            "create sequence s global shard extend scale extend keep order cache 20 cycle;",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun rejectsNonCreateSequenceSyntax() {
        // ORA-11543 / ORA-00922 / ORA-64602
        assertThat(p).notMatches("create sequence if exists seq_name;")
        assertThat(p).notMatches("create or replace sequence seq_name;")
        assertThat(p).notMatches("create editionable sequence seq_name;")
        assertThat(p).notMatches("create sequence seq_name restart;")
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
        assertThat(p).notMatches("create sequence seq_name scale unexpected;")
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
