SELECT department_id, last_name, salary 
   FROM employees x 
   WHERE salary > (SELECT AVG(salary) 
      FROM employees 
      WHERE x.department_id = department_id) 
   ORDER BY department_id;