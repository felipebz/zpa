SELECT /*+ FULL(e) */ employee_id, last_name
  FROM hr.employees e 
  WHERE last_name LIKE :b1;