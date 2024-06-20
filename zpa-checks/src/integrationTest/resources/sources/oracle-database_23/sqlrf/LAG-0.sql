-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/LAG.html
SELECT hire_date, last_name, salary,
       LAG(salary, 1, 0) OVER (ORDER BY hire_date) AS prev_sal
  FROM employees
  WHERE job_id = 'PU_CLERK'
  ORDER BY hire_date;