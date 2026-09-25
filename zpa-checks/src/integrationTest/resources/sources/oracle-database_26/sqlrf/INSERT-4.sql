-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/INSERT.html
  INSERT INTO job_history (employee_id, start_date, end_date, department_id, job_id)
  SELECT employee_id, hire_date, SYSDATE - 1, department_id, job_id FROM employees
  WHERE employee_id = 206;