-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Format-Models.html
SELECT last_name employee, TO_CHAR(salary, '$99,990.99')
  FROM employees
  WHERE department_id = 80;