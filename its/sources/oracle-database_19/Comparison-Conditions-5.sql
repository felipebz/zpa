-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comparison-Conditions.html
SELECT * FROM employees
  WHERE salary >=
  ALL (1400, 3000)
  ORDER BY employee_id;