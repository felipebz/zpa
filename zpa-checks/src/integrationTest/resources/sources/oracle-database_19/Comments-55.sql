-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comments.html
SELECT /*+ USE_CONCAT */ *
  FROM employees e
  WHERE manager_id = 108
     OR department_id = 110;