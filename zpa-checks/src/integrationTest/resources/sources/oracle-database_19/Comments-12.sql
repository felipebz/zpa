-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comments.html
SELECT /*+ FULL(e) */ employee_id, last_name
  FROM hr.employees e 
  WHERE last_name LIKE :b1;