SELECT * FROM departments 
   WHERE EXISTS 
   (SELECT * FROM employees 
       WHERE departments.department_id = employees.department_id 
       AND employees.salary > 2500)
   ORDER BY department_name;