-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/APPROX_COUNT.html
SELECT department_id, job_id, 
       APPROX_COUNT(*) 
FROM   employees
GROUP BY department_id, job_id
HAVING 
  APPROX_RANK ( 
  PARTITION BY department_id 
  ORDER BY APPROX_COUNT(*) 
  DESC ) <= 10;