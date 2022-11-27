-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
SELECT last_name, department_id FROM employees
   WHERE department_id =
     (SELECT department_id FROM employees
      WHERE last_name = 'Lorentz')
   ORDER BY last_name, department_id;