-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/IN-Condition.html
SELECT 'True' FROM employees
   WHERE department_id NOT IN (10, 20);