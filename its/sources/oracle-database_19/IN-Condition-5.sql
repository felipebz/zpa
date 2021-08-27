-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/IN-Condition.html
SELECT 'True' FROM employees
    WHERE department_id NOT IN (10, 20, NULL);