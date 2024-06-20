-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
CREATE TABLE dept_80
   AS SELECT * FROM employees
   WHERE department_id = 80;