-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-LIBRARY-statement.html
CREATE LIBRARY app_lib as '${ORACLE_HOME}/lib/app_lib.so'
   AGENT 'sales.hq.example.com';
/