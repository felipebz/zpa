-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/UPDATE.html
UPDATE employees e  
SET    e.salary = j.max_salary
FROM   jobs j
WHERE  j.job_id = e.job_id;