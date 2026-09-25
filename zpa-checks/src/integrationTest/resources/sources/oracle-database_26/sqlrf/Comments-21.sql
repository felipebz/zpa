-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Comments.html
SELECT /*+ INDEX_SS_DESC(e emp_name_ix) */ last_name
  FROM employees e
  WHERE first_name = 'Steven';