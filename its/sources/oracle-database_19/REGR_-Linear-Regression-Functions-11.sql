-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/REGR_-Linear-Regression-Functions.html
SELECT job_id,
REGR_R2(SYSDATE-hire_date, salary) Regr_R2
   FROM employees
   WHERE department_id in (80, 50)
   GROUP by job_id
  ORDER BY job_id, Regr_R2;