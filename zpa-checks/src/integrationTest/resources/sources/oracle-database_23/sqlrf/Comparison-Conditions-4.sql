-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Comparison-Conditions.html
SELECT * FROM employees
  WHERE salary = ANY
  (SELECT salary 
   FROM employees
  WHERE department_id = 30)
  ORDER BY employee_id;