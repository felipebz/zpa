SELECT employee_id, last_name FROM employees
   WHERE (employee_id, LEVEL) 
      IN (SELECT employee_id, 2 FROM employees)
   START WITH employee_id = 2
   CONNECT BY PRIOR employee_id = manager_id;