-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/STDDEV_SAMP.html
SELECT department_id, last_name, hire_date, salary, 
   STDDEV_SAMP(salary) OVER (PARTITION BY department_id 
      ORDER BY hire_date 
      ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cum_sdev 
   FROM employees
   ORDER BY department_id, last_name, hire_date, salary, cum_sdev;