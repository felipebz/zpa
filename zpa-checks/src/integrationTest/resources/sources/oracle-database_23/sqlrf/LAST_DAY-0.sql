-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/LAST_DAY.html
SELECT SYSDATE,
       LAST_DAY(SYSDATE) "Last",
       LAST_DAY(SYSDATE) - SYSDATE "Days Left"
  FROM DUAL;