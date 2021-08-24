SELECT * FROM employees
  WHERE salary >=
  ALL (1400, 3000)
  ORDER BY employee_id;