-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/STATS_MODE.html
SELECT department_id, STATS_MODE(salary) FROM employees
   GROUP BY department_id
   ORDER BY department_id, stats_mode(salary);