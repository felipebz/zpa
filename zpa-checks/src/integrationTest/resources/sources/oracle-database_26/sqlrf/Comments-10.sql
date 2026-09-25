-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Comments.html
SELECT /*+ DYNAMIC_SAMPLING(e 1) */ count(*)
  FROM employees e;