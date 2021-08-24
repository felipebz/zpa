SELECT AVG(MAX(salary))
  FROM employees
  GROUP BY department_id;