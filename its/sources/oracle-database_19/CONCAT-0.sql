SELECT CONCAT(CONCAT(last_name, '''s job category is '), job_id) "Job" 
  FROM employees 
  WHERE employee_id = 152;