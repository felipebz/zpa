-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/APPROX_COUNT_DISTINCT.html
SELECT APPROX_COUNT_DISTINCT(manager_id) AS "Active Managers"
  FROM employees;