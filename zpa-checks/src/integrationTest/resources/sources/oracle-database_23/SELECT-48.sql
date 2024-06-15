-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT * 
   FROM employees
   WHERE job_id = 'PU_CLERK' 
   ORDER BY salary DESC;