-- The following statement is not valid:
SELECT employee_id, manager_id 
   FROM employees
   WHERE employees.manager_id(+) = employees.employee_id;