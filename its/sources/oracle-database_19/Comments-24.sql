SELECT /*+ NO_INDEX(employees emp_empid) */ employee_id 
  FROM employees 
  WHERE employee_id > 200;