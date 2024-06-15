-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_TIMESTAMP_TZ.html
SELECT TO_TIMESTAMP_TZ('1999-12-01 11:00:00 -8:00',
   'YYYY-MM-DD HH:MI:SS TZH:TZM') FROM DUAL;