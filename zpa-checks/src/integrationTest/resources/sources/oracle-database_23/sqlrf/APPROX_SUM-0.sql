-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/APPROX_SUM.html
SELECT department_id, job_id, 
       APPROX_SUM(salary) 
FROM   employees
GROUP BY department_id, job_id
HAVING 
  APPROX_RANK ( 
  PARTITION BY department_id 
  ORDER BY APPROX_SUM(salary) 
  DESC ) <= 10;