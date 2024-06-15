-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT last_name, employee_id, manager_id FROM employees
   CONNECT BY employee_id = manager_id
   ORDER BY last_name;