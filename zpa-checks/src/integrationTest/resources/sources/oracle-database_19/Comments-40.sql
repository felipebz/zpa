-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comments.html
SELECT /*+ PARALLEL */ last_name
  FROM employees;