-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html
SELECT JOB, AVG(SAL)
FROM EMP
GROUP BY JOB
HAVING JOB IS NOT NULL
QUALIFY AVG(SAL) < 2000;