-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-OUTLINE.html
CREATE OUTLINE salaries FOR CATEGORY special
   ON SELECT last_name, salary FROM employees;