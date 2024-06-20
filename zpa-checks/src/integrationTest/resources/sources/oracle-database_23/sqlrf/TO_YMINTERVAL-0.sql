-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_YMINTERVAL.html
SELECT hire_date, hire_date + TO_YMINTERVAL('01-02') "14 months"
   FROM employees;