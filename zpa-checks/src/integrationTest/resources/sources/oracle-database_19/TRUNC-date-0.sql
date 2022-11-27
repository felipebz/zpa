-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TRUNC-date.html
SELECT TRUNC(TO_DATE('27-OCT-92','DD-MON-YY'), 'YEAR')
  "New Year" FROM DUAL;