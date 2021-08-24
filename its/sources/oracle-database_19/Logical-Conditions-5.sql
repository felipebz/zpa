SELECT *
  FROM employees
  WHERE job_id = 'PU_CLERK'
  OR department_id = 10
  ORDER BY employee_id;