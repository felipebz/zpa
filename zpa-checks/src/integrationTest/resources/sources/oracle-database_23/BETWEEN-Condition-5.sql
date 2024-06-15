-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/BETWEEN-Condition.html
SELECT * FROM employees
  WHERE salary
  BETWEEN 2000 AND 3000
  ORDER BY employee_id;