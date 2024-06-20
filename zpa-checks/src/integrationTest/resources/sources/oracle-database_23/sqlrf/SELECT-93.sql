-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT * FROM departments 
   WHERE EXISTS 
   (SELECT * FROM employees 
       WHERE departments.department_id = employees.department_id 
       AND employees.salary > 2500)
   ORDER BY department_name;