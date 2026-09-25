-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html
  INSERT INTO job_history
  BY NAME
  SELECT employee_id, hire_date AS start_date, SYSDATE - 1 AS end_date, department_id, job_id FROM employees
  WHERE employee_id = 206