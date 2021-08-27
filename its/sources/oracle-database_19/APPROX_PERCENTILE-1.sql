-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/APPROX_PERCENTILE.html
SELECT department_id "Department",
       APPROX_PERCENTILE(0.25 DETERMINISTIC, 'ERROR_RATE')
         WITHIN GROUP (ORDER BY salary ASC) "Error Rate"
  FROM employees
  GROUP BY department_id
  ORDER BY department_id;