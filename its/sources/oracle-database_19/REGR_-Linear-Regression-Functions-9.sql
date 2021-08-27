-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/REGR_-Linear-Regression-Functions.html
SELECT job_id,
REGR_SLOPE(SYSDATE-hire_date, salary) slope,
REGR_INTERCEPT(SYSDATE-hire_date, salary) intercept
   FROM employees
   WHERE department_id in (50,80)
   GROUP BY job_id
   ORDER BY job_id;