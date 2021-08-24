SELECT /*+ USE_MERGE(employees departments) */ * 
  FROM employees, departments 
  WHERE employees.department_id = departments.department_id;