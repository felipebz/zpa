-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
SELECT e1.last_name||' works for '||e2.last_name 
   "Employees and Their Managers"
   FROM employees e1, employees e2 
   WHERE e1.manager_id = e2.employee_id
      AND e1.last_name LIKE 'R%'
   ORDER BY e1.last_name;