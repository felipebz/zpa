//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectPlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectTestSource
import org.junit.jupiter.api.Test

class ParameterInDynamicSqlCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        PlSqlCheckVerifier.verify(getPath("parameter_in_dynamic_sql.sql"), ParameterInDynamicSqlCheck())
    }

    @Test
    fun aVariablePassedToAnInFormalOfAnotherFileKeepsItsValue() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "shelf_log.pks",
                    """
                    create package shelf_log as
                      procedure note(p_text varchar2);
                      procedure keep(p_text in out varchar2);
                      procedure twice(p_text varchar2);
                      procedure twice(p_text char);
                    end;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "find.sql",
                    """
                    create procedure find_crate(p_code varchar2) is
                      n number;
                      v_sql varchar2(200) := 'select count(*) from crate where code = ''' || p_code || ''''; -- Noncompliant
                      v_kept varchar2(200) := 'select count(*) from crate where code = ''' || p_code || '''';
                      v_two varchar2(200) := 'select count(*) from crate where code = ''' || p_code || '''';
                      v_lost varchar2(200) := 'select count(*) from crate where code = ''' || p_code || '''';
                    begin
                      shelf_log.note(v_sql);
                      execute immediate v_sql into n;
                      shelf_log.keep(v_kept);
                      execute immediate v_kept into n;
                      shelf_log.twice(v_two);
                      execute immediate v_two into n;
                      zz_missing_log.note(v_lost);
                      execute immediate v_lost into n;
                    end;
                    /
                    """.trimIndent()
                )
            ),
            ParameterInDynamicSqlCheck()
        )
    }
}
