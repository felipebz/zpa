-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/MAX.html
SELECT manager_id, last_name, salary,
       MAX(salary) OVER (PARTITION BY manager_id) AS mgr_max
  FROM employees
  ORDER BY manager_id, last_name, salary;