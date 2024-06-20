-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT TRUNC(hire_date, 'YYYY') hire_date, COUNT(*)
FROM employees
GROUP BY hire_date
ORDER BY hire_date;