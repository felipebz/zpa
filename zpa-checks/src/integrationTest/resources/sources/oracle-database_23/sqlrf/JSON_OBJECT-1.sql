-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_OBJECT.html
SELECT JSON_OBJECT(*)
FROM employees
WHERE employee_id = 140;