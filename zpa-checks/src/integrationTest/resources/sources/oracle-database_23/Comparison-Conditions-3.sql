-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Comparison-Conditions.html
SELECT * FROM employees
  WHERE salary >= 2500
  ORDER BY employee_id;
SELECT * FROM employees
  WHERE salary <= 2500
  ORDER BY employee_id;