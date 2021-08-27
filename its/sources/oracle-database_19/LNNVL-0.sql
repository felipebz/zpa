-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/LNNVL.html
SELECT COUNT(*)
  FROM employees
  WHERE commission_pct < .2;