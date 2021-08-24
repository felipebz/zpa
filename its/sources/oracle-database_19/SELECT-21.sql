SELECT last_name, job_id, salary, department_id 
   FROM employees 
   WHERE NOT (job_id = 'PU_CLERK' AND department_id = 30)
   ORDER BY last_name;