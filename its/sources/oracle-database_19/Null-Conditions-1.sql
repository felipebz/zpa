SELECT last_name
  FROM employees
  WHERE commission_pct
  IS NULL
  ORDER BY last_name;