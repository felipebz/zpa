-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-TABLE.html
SELECT employee_id, last_name, department_id
  FROM employees
  WHERE department_id IS NULL;