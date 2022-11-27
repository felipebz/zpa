-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/EXISTS-Condition.html
SELECT department_id
  FROM departments d
  WHERE EXISTS
  (SELECT * FROM employees e
    WHERE d.department_id 
    = e.department_id)
   ORDER BY department_id;