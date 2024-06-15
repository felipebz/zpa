-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Comments.html
ALTER TABLE employees PARALLEL 8;
SELECT /*+ NO_PARALLEL(hr_emp) */ last_name
  FROM employees hr_emp;