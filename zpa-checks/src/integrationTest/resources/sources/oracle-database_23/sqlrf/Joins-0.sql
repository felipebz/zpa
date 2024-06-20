-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Joins.html
SELECT employee_id, manager_id 
   FROM employees
   WHERE employees.manager_id(+) = employees.employee_id;