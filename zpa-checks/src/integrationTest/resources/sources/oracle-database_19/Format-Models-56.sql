-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Format-Models.html
SELECT last_name employee, TO_CHAR(hire_date,'fmMonth DD, YYYY') hiredate
  FROM employees
  WHERE department_id = 20;