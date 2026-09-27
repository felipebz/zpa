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

class ParameterFileTest : RuleTest() {

    @Test
    fun matchesCreatePfile() {
        setRootRule(DdlGrammar.CREATE_PFILE)
        assertThat(p).matches("create pfile = 'my_init.ora' from spfile = 's_params.ora';")
        assertThat(p).matches("create pfile from spfile = 's_params.ora';")
        assertThat(p).matches("create pfile = 'my_init.ora' from spfile;")
        assertThat(p).matches("create pfile from spfile")
        assertThat(p).matches("create pfile = 'my_init.ora' from memory;")
        assertThat(p).matches("create pfile from memory")
    }

    @Test
    fun rejectsInvalidCreatePfile() {
        setRootRule(DdlGrammar.CREATE_PFILE)
        // Missing FROM (ORA-00923).
        assertThat(p).notMatches("create pfile = 'my_init.ora'")
        assertThat(p).notMatches("create pfile")
        // AS COPY not valid on CREATE PFILE (ORA-03048).
        assertThat(p).notMatches("create pfile = 'my_init.ora' from spfile = 's_params.ora' as copy")
        assertThat(p).notMatches("create pfile = 'my_init.ora' from memory as copy")
        // FROM PFILE not valid on CREATE PFILE (ORA-00922).
        assertThat(p).notMatches("create pfile from pfile")
        // Non-literal or missing `=` (ORA-02236 / ORA-03046).
        assertThat(p).notMatches("create pfile = my_init from spfile")
        assertThat(p).notMatches("create pfile 'my_init.ora' from spfile")
        assertThat(p).notMatches("create pfile = 'my_init.ora' from spfile 's_params.ora'")
        assertThat(p).notMatches("create pfile = 'my_init.ora' from spfile = s_params")
        // OR REPLACE rejected (ORA-00922).
        assertThat(p).notMatches("create or replace pfile from spfile")
    }

    @Test
    fun matchesCreateSpfile() {
        setRootRule(DdlGrammar.CREATE_SPFILE)
        assertThat(p).matches("create spfile from pfile = '\$ORACLE_HOME/work/t_init1.ora';")
        assertThat(p).matches("create spfile = 's_params.ora' from pfile = '\$ORACLE_HOME/work/t_init1.ora';")
        assertThat(p).matches("create spfile = 's_params.ora' from pfile = '\$ORACLE_HOME/work/t_init1.ora' as copy;")
        assertThat(p).matches("create spfile = 's_params.ora' from pfile;")
        assertThat(p).matches("create spfile from pfile")
        assertThat(p).matches("create spfile from pfile as copy")
        assertThat(p).matches("create spfile = 's_params.ora' from memory;")
        assertThat(p).matches("create spfile from memory")
        assertThat(p).matches("create spfile from memory as copy")
    }

    @Test
    fun rejectsInvalidCreateSpfile() {
        setRootRule(DdlGrammar.CREATE_SPFILE)
        // Missing FROM (ORA-00923).
        assertThat(p).notMatches("create spfile = 's_params.ora'")
        assertThat(p).notMatches("create spfile")
        // FROM SPFILE not valid on CREATE SPFILE (ORA-00922).
        assertThat(p).notMatches("create spfile from spfile")
        // Non-literal or missing `=` (ORA-02236 / ORA-00923).
        assertThat(p).notMatches("create spfile = s_params from pfile")
        assertThat(p).notMatches("create spfile 's_params.ora' from pfile")
        assertThat(p).notMatches("create spfile from pfile 'my_init.ora'")
        assertThat(p).notMatches("create spfile from pfile = my_init")
        // OR REPLACE rejected (ORA-00922).
        assertThat(p).notMatches("create or replace spfile from pfile")
    }
}
