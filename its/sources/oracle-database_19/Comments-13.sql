SELECT /*+ INDEX (employees emp_department_ix)*/ employee_id, department_id
  FROM employees 
  WHERE department_id > 50;