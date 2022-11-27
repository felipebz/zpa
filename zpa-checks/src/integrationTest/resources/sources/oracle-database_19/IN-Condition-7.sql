-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/IN-Condition.html
SELECT 'True' FROM employees
   WHERE department_id NOT IN (SELECT 0 FROM DUAL WHERE 1=2);