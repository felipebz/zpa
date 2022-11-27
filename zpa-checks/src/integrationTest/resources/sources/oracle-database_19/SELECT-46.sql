-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
SELECT employee_id, last_name
  FROM employees
  ORDER BY employee_id
  FETCH FIRST 5 ROWS ONLY;