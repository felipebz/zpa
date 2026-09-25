-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html
SELECT SAL
FROM EMP
WHERE JOB IS NOT NULL
QUALIFY SAL < 2000;