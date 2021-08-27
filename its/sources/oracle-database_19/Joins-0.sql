-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Joins.html
-- The following statement is not valid:
SELECT employee_id, manager_id 
   FROM employees
   WHERE employees.manager_id(+) = employees.employee_id;