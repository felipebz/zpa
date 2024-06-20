-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
SELECT employee_id, last_name, department_id
  FROM employees
  WHERE department_id IS NULL;