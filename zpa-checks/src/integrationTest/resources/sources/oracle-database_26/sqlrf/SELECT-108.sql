-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html
SELECT last_name, department_name 
   FROM employees@remote, departments
   WHERE employees.department_id = departments.department_id;