-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comments.html
SELECT /*+ INDEX_FFS(e emp_name_ix) */ first_name
  FROM employees e;