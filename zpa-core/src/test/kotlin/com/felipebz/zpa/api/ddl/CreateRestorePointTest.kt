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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateRestorePointTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_RESTORE_POINT)
    }

    @Test
    fun matchesSimpleForms() {
        assertThat(p).matches("create restore point good_data;")
        assertThat(p).matches("create restore point good_data")
        assertThat(p).matches("create restore point \"Rp 1\"")
        assertThat(p).matches("create clean restore point rp1")
    }

    @Test
    fun matchesEachClause() {
        assertThat(p).matches("create restore point rp2 for pluggable database pdb")
        assertThat(p).matches("create restore point rp2 for pluggable database \"Pdb\"")
        assertThat(p).matches("create restore point rp2 preserve")
        assertThat(p).matches("create restore point rp2 guarantee flashback database")
        assertThat(p).matches("create restore point rp2 as of scn 100")
        assertThat(p).matches("create restore point rp2 as of scn 100 + 1")
        assertThat(p).matches("create restore point rp2 as of timestamp systimestamp - interval '1' minute")
        assertThat(p).matches("create restore point rp2 as of timestamp to_timestamp('2020-01-01', 'yyyy-mm-dd')")
    }

    @Test
    fun matchesCombinationsInOracleOrder() {
        assertThat(p).matches("create restore point rp2 for pluggable database pdb preserve;")
        assertThat(p).matches("create clean restore point rp1 for pluggable database pdb as of scn 1 preserve")
        assertThat(p).matches("create restore point rp2 for pluggable database pdb guarantee flashback database")
        // GUARANTEE with AS OF parses; Oracle rejects it afterwards (ORA-38864).
        assertThat(p).matches("create restore point rp2 as of scn 1 guarantee flashback database")
        // CLEAN without a PDB parses; Oracle restricts it semantically.
        assertThat(p).matches("create clean restore point rp1 guarantee flashback database")
    }

    @Test
    fun rejectsWrongOrderAndDuplicates() {
        assertThat(p).notMatches("create restore point rp2 as of scn 1 for pluggable database pdb")
        assertThat(p).notMatches("create restore point rp2 preserve for pluggable database pdb")
        assertThat(p).notMatches("create restore point rp2 preserve as of scn 1")
        assertThat(p).notMatches("create restore point rp2 preserve guarantee flashback database")
        assertThat(p).notMatches("create restore point rp2 guarantee flashback database preserve")
        assertThat(p).notMatches("create restore point rp2 preserve preserve")
        assertThat(p).notMatches("create restore point rp2 as of scn 1 as of timestamp 1")
        assertThat(p).notMatches("create restore point rp2 for pluggable database pdb for pluggable database pdb")
        assertThat(p).notMatches("create restore point rp2 for pluggable database pdb clean")
        assertThat(p).notMatches("create clean clean restore point rp1")
    }

    @Test
    fun rejectsMalformedClauses() {
        assertThat(p).notMatches("create restore point")
        assertThat(p).notMatches("create restore point for pluggable database pdb")
        assertThat(p).notMatches("create restore point rp2 for database pdb")
        assertThat(p).notMatches("create restore point rp2 for pluggable database")
        assertThat(p).notMatches("create restore point rp2 for pluggable database pdb, pdb2")
        assertThat(p).notMatches("create restore point rp2 guarantee")
        assertThat(p).notMatches("create restore point rp2 guarantee flashback")
        assertThat(p).notMatches("create restore point rp2 noguarantee")
        assertThat(p).notMatches("create restore point rp2 as of 1")
        assertThat(p).notMatches("create restore point rp2 as of scn")
        assertThat(p).notMatches("create restore point rp2 as scn 1")
    }

    @Test
    fun rejectsQualifiedNamesAndUnsupportedModifiers() {
        assertThat(p).notMatches("create restore point a.b")
        assertThat(p).notMatches("create restore point rp2@dblink")
        assertThat(p).notMatches("create restore point rp2 for pluggable database a.pdb")
        assertThat(p).notMatches("create restore point rp2 for pluggable database pdb@dblink")
        // ORA-11600: IF [NOT] EXISTS is not supported for this DDL.
        assertThat(p).notMatches("create restore point if not exists rp2")
        assertThat(p).notMatches("create or replace restore point rp2")
    }

}
