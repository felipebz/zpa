-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Comments.html
SELECT /*+ FULL(hr_emp) NOCACHE(hr_emp) */ last_name
  FROM employees hr_emp;