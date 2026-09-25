-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/ROWNUM-Pseudocolumn.html
SELECT *
  FROM employees
  WHERE ROWNUM < 11;