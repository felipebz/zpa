-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT TRUNC(hire_date, 'YYYY') year_hired, COUNT(*)
FROM employees
GROUP BY year_hired
ORDER BY year_hired;