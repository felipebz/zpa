SELECT /*+ DRIVING_SITE(departments) */ * 
  FROM employees, departments@rsite 
  WHERE employees.department_id = departments.department_id;