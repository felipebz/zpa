-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Aggregate-Functions.html
SELECT AVG(MAX(salary))
  FROM employees
  GROUP BY department_id;