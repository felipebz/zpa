-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CUME_DIST.html
SELECT job_id, last_name, salary, CUME_DIST() 
  OVER (PARTITION BY job_id ORDER BY salary) AS cume_dist
  FROM employees
  WHERE job_id LIKE 'PU%'
  ORDER BY job_id, last_name, salary, cume_dist;