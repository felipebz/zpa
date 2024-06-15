-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT * FROM employees 
   WHERE department_id NOT IN 
   (SELECT department_id FROM departments 
       WHERE location_id = 1700)
   ORDER BY last_name;