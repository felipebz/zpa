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
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class CreateLedgerTableTest : RuleTest() {

    private val blockchain = "create blockchain table t (a number, b varchar2(10))"
    private val immutable = "create immutable table t (a number, b varchar2(10))"
    private val retention = "no drop until 0 days idle no delete until 16 days after insert"

    @Test
    fun matchesBlockchainTables() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches(
            "create blockchain table bank_ledger (bank varchar2(128), account_no number) " +
                "no drop until 31 days idle no delete locked hashing using sha2_512 version v2;")
        assertThat(p).matches("$blockchain no drop no delete hashing using sha2_512 version v1")
        assertThat(p).matches("$blockchain $retention locked hashing using sha2_512 version v2")
        assertThat(p).matches("$blockchain $retention hashing using sha2_512 with row version rv (a, b) version v2")
        assertThat(p).matches("$blockchain $retention hashing using sha2_512 with row version \"Rv\" ((a), (b)) version v2")
        assertThat(p).matches(
            "$blockchain $retention hashing using sha2_512 with row version and user chain rv (a, b) version v2")
        assertThat(p).matches("$blockchain $retention hashing using sha2_512 with user chain uc (a) version v2")
        assertThat(p).matches(
            "$blockchain $retention hashing using sha2_512 with row version rv (a) " +
                "configure 4 system chains per instance version v2")
        assertThat(p).matches("create immutable blockchain table t (a number) $retention hashing using sha2_512 version v2")
        // Unsupported algorithms and versions fail later (ORA-05716/ORA-05770).
        assertThat(p).matches("$blockchain $retention hashing using sha2_256 version v3")
    }

    @Test
    fun rejectsInvalidBlockchainClauses() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).notMatches(blockchain)
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512")
        assertThat(p).notMatches("$blockchain no drop no delete version v2")
        assertThat(p).notMatches("$blockchain no delete hashing using sha2_512 version v2")
        assertThat(p).notMatches("$blockchain no drop hashing using sha2_512 version v2")
        assertThat(p).notMatches("$blockchain no delete no drop hashing using sha2_512 version v2")
        assertThat(p).notMatches("$blockchain hashing using sha2_512 no drop no delete version v2")
        assertThat(p).notMatches("$blockchain no drop no delete version v2 hashing using sha2_512")
        assertThat(p).notMatches("$blockchain no drop no drop no delete hashing using sha2_512 version v2")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 version v2 version v2")
        // Literals are rejected during parsing (ORA-05700).
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 version 2")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 version 'v2'")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using 'sha2_512' version v2")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 with row version (a) version v2")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 with user chain and row version uc (a) version v2")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 with row version rv a, b version v2")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 with row version rv () version v2")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 with row version rv (a,) version v2")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 with row version s.rv (a) version v2")
        assertThat(p).notMatches(
            "$blockchain no drop no delete hashing using sha2_512 with row version rv (a) with user chain uc (b) version v2")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 configure 4 system chains version v2")
        assertThat(p).notMatches(
            "$blockchain no drop no delete hashing using sha2_512 configure 4 system chains per instance " +
                "with row version rv (a) version v2")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 version v2 with row version rv (a)")
        assertThat(p).notMatches("$blockchain no drop until 0 days no delete hashing using sha2_512 version v2")
        assertThat(p).notMatches("create global temporary blockchain table t (a number) $retention hashing using sha2_512 version v2")
    }

    @Test
    fun matchesImmutableTables() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches(
            "create immutable table trade_ledger (tr_id number, user_name varchar2(40), tr_value number) " +
                "no drop until 40 days idle no delete until 100 days after insert;")
        assertThat(p).matches("$immutable no drop no delete")
        assertThat(p).matches("$immutable no drop until 0 days idle no delete locked")
        assertThat(p).matches("$immutable $retention locked version v1")
        assertThat(p).matches("$immutable $retention with row version rv (a, b)")
        assertThat(p).matches("$immutable $retention with row version rv (a) version v2")
    }

    @Test
    fun rejectsInvalidImmutableClauses() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).notMatches(immutable)
        assertThat(p).notMatches("$immutable no drop until 0 days idle")
        assertThat(p).notMatches("$immutable no delete until 16 days after insert")
        assertThat(p).notMatches("$immutable no delete no drop")
        assertThat(p).notMatches("$immutable version v2")
        assertThat(p).notMatches("$immutable $retention version v2 with row version rv (a)")
        assertThat(p).notMatches("$immutable $retention with row version rv a, b")
        assertThat(p).notMatches("$immutable $retention with row version and user chain rv (a)")
        assertThat(p).notMatches("$immutable $retention with user chain uc (a)")
        assertThat(p).notMatches("$immutable $retention hashing using sha2_512")
        assertThat(p).notMatches("$immutable $retention configure 4 system chains per instance version v2")
        assertThat(p).notMatches("$immutable no drop until 0 days no delete")
        assertThat(p).notMatches("$immutable no drop no delete until 16 days")
        assertThat(p).notMatches("$immutable no drop no delete locked until 16 days after insert")
    }

    @Test
    fun placesLedgerClausesBeforeOtherProperties() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("$immutable $retention tablespace users")
        assertThat(p).matches("$immutable $retention partition by hash (a) partitions 2 for staging")
        assertThat(p).matches("$blockchain $retention hashing using sha2_512 version v2 inmemory enable row movement")
        assertThat(p).matches("create immutable table t $retention as select 1 a from dual")
        assertThat(p).matches("create blockchain table t $retention hashing using sha2_512 version v2 as select 1 a from dual")
        assertThat(p).notMatches("create immutable table t (a number) tablespace users $retention")
        assertThat(p).notMatches("create immutable table t (a number) partition by hash (a) partitions 2 $retention")
        // Ordinary tables do not take the ledger clauses.
        assertThat(p).notMatches("create table t (a number) $retention")
    }

    @Test
    fun acceptsMissingCountsValidatedAfterParsing() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        // Oracle reports these as value-range errors (ORA-05741/ORA-05804), not syntax errors.
        assertThat(p).matches("$immutable no drop until days idle no delete until days after insert;")
        assertThat(p).matches(
            "$blockchain no drop no delete hashing using sha2_512 configure system chains per instance version v2;")
        assertThat(p).matches("alter table t no drop until days idle;")
        assertThat(p).matches("alter table t no delete until days after insert locked;")
        // The keywords around the count stay required.
        assertThat(p).notMatches("$immutable no drop until idle no delete;")
        assertThat(p).notMatches("$immutable no drop until 0 days no delete;")
        assertThat(p).notMatches("$immutable no drop no delete until days insert;")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 configure chains per instance version v2;")
        assertThat(p).notMatches("$blockchain no drop no delete hashing using sha2_512 configure x system chains per instance version v2;")
    }

    @Test
    fun rejectsUndocumentedBlockchainImmutablePrefix() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        // Not in the Oracle 26 syntax; the runtime answers every CREATE BLOCKCHAIN not followed by TABLE with
        // ORA-00439 (feature not enabled) before reading the rest, so it gives no evidence either way.
        assertThat(p).notMatches("create blockchain immutable table t (a number) $retention hashing using sha2_512 version v2")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("$immutable $retention;")
        assertThat(p).matches("$blockchain $retention hashing using sha2_512 version v2;")
        assertThat(p).matches("alter table t no drop until 10 days idle;")
    }
}
