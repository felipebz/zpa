-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/APPROX_MEDIAN.html
SELECT department_id "Department",
       APPROX_MEDIAN(salary DETERMINISTIC, 'ERROR_RATE') "Error Rate"
  FROM employees
  GROUP BY department_id
  ORDER BY department_id;