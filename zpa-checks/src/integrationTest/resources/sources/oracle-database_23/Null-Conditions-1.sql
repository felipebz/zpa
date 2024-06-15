-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Null-Conditions.html
SELECT last_name
  FROM employees
  WHERE commission_pct
  IS NULL
  ORDER BY last_name;