-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overloaded-subprograms.html
ALTER SESSION SET PLSQL_IMPLICIT_CONVERSION_BOOL = TRUE;
exec pkg1.s('1');  -- Causes compile-time error PLS-00307
