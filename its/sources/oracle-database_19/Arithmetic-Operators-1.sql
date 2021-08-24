SELECT hire_date 
  FROM employees
  WHERE SYSDATE - hire_date > 365
  ORDER BY hire_date;