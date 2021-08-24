SELECT *
  FROM employees
  WHERE ROWNUM < 11
  ORDER BY last_name;