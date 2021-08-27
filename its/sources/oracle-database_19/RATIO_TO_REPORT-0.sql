-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/RATIO_TO_REPORT.html
SELECT last_name, salary, RATIO_TO_REPORT(salary) OVER () AS rr
   FROM employees
   WHERE job_id = 'PU_CLERK'
   ORDER BY last_name, salary, rr;