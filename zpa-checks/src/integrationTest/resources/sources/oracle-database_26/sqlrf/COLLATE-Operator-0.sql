-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COLLATE-Operator.html
SELECT last_name
  FROM employees
  ORDER BY last_name COLLATE GENERIC_M;