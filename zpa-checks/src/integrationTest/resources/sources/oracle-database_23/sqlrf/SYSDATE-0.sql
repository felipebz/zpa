-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SYSDATE.html
SELECT TO_CHAR
    (SYSDATE, 'MM-DD-YYYY HH24:MI:SS') "NOW"
     FROM DUAL;