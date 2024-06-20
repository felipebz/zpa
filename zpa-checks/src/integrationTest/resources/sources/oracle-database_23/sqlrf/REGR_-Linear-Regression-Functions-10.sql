-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGR_-Linear-Regression-Functions.html
SELECT job_id,
REGR_COUNT(SYSDATE-hire_date, salary) count
   FROM employees
   WHERE department_id in (30, 50)
   GROUP BY job_id
   ORDER BY job_id, count;