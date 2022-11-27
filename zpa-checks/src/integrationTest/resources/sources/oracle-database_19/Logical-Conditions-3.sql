-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Logical-Conditions.html
SELECT *
  FROM employees
  WHERE job_id = 'PU_CLERK'
  AND department_id = 30
  ORDER BY employee_id;