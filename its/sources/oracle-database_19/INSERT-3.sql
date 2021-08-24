INSERT INTO 
   (SELECT employee_id, last_name, email, hire_date, job_id, 
      salary, commission_pct FROM employees) 
   VALUES (207, 'Gregory', 'pgregory@example.com', 
      sysdate, 'PU_CLERK', 1.2E3, NULL);