-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Comments.html
SELECT /*+ PARALLEL (AUTO) */ last_name
  FROM employees;