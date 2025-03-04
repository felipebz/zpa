-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/compile-time-warnings.html
ALTER SESSION
  SET PLSQL_WARNINGS='ENABLE:SEVERE', 'DISABLE:PERFORMANCE', 'ERROR:06002';