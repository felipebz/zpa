//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
