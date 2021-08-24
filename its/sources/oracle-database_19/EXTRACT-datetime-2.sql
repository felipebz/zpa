SELECT last_name, employee_id, hire_date
  FROM employees
  WHERE EXTRACT(YEAR FROM (hire_date, 'DD-MON-RR')) > 2007
  ORDER BY hire_date;