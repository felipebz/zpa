-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/EXTRACT-datetime.html
SELECT last_name, employee_id, hire_date
  FROM employees
  WHERE EXTRACT(YEAR FROM hire_date) > 2007
  ORDER BY hire_date;