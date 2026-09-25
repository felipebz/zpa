-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html
SELECT employee_id FROM (SELECT * FROM employees)
   FOR UPDATE OF employee_id;