-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/NTILE.html
SELECT last_name, salary, NTILE(4) OVER (ORDER BY salary DESC) AS quartile
  FROM employees
  WHERE department_id = 100
  ORDER BY last_name, salary, quartile;