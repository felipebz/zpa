-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comments.html
SELECT /*+ INDEX (employees emp_department_ix)*/ employee_id, department_id
  FROM employees 
  WHERE department_id > 50;