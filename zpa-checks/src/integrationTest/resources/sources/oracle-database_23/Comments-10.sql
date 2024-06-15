-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Comments.html
SELECT /*+ DYNAMIC_SAMPLING(employees 1) */ *
  FROM employees 
  WHERE ...