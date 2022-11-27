-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/MEDIAN.html
SELECT manager_id, employee_id, salary,
       MEDIAN(salary) OVER (PARTITION BY manager_id) "Median by Mgr"
  FROM employees
  WHERE department_id > 60
  ORDER BY manager_id, employee_id;