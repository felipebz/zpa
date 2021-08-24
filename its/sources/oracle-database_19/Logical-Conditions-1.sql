SELECT *
  FROM employees
  WHERE NOT (job_id IS NULL)
  ORDER BY employee_id;
SELECT *
  FROM employees
  WHERE NOT 
  (salary BETWEEN 1000 AND 2000)
  ORDER BY employee_id;