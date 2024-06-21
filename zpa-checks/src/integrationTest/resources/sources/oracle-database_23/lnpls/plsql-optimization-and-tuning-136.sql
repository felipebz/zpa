-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
ALTER PROCEDURE my_proc COMPILE PLSQL_CODE_TYPE=NATIVE REUSE SETTINGS;