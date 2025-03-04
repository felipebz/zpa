-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/compile-time-warnings.html
ALTER PROCEDURE loc_var
  COMPILE PLSQL_WARNINGS='ENABLE:PERFORMANCE'
  REUSE SETTINGS;