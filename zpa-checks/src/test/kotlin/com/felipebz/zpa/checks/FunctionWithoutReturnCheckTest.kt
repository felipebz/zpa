//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.zpa.checks.verifier.PlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectPlSqlCheckVerifier
import com.felipebz.zpa.checks.verifier.ProjectTestSource
import org.junit.jupiter.api.Test

class FunctionWithoutReturnCheckTest : BaseCheckTest() {

    @Test
    fun test() {
        PlSqlCheckVerifier.verify(getPath("function_without_return.sql"), FunctionWithoutReturnCheck())
    }

    @Test
    fun aCallIntoAnotherFileIsNotProofThatExecutionGoesOn() {
        ProjectPlSqlCheckVerifier.verify(
            listOf(
                ProjectTestSource(
                    "bin_err.sql",
                    """
                    create or replace package bin_err as
                      procedure raise_error(p varchar2);
                    end;
                    /

                    create or replace package body bin_err as
                      procedure raise_error(p varchar2) is
                      begin
                        raise_application_error(-20001, p);
                      end;
                    end;
                    /
                    """.trimIndent()
                ),
                ProjectTestSource(
                    "bin_crate.sql",
                    """
                    create or replace function crate_code(p_id number) return varchar2 is
                      v varchar2(10);
                    begin
                      select code into v from bin_crate where id = p_id;
                      return v;
                    exception
                      when others then
                        bin_err.raise_error('crate not found');
                    end;
                    /

                    create or replace function crate_code_or_null(p_id number) return varchar2 is -- Noncompliant
                      v varchar2(10);
                    begin
                      select code into v from bin_crate where id = p_id;
                      return v;
                    exception
                      when others then
                        bin_err.raise_error('crate not found');
                        v := null;
                    end;
                    /
                    """.trimIndent()
                )
            ),
            FunctionWithoutReturnCheck()
        )
    }

}
