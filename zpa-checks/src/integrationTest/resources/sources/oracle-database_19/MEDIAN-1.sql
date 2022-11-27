-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/MEDIAN.html
SELECT department_id, MEDIAN(salary)
  FROM employees
  GROUP BY department_id
  ORDER BY department_id;