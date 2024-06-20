-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ROWNUM-Pseudocolumn.html
SELECT *
  FROM employees
  WHERE ROWNUM < 11
  ORDER BY last_name;