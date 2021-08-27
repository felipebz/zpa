-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_YMINTERVAL.html
SELECT hire_date, hire_date + TO_YMINTERVAL('P1Y2M') FROM employees;