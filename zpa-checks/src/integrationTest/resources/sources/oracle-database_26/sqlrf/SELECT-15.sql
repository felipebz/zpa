-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html
SELECT avgsal
   FROM (SELECT AVG(salary) AS avgsal FROM employees GROUP BY job_id)
   FOR UPDATE;