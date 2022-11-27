-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ROWNUM-Pseudocolumn.html
SELECT *
  FROM (SELECT * FROM employees ORDER BY employee_id)
  WHERE ROWNUM < 11;