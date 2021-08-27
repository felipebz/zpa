-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Floating-Point-Conditions.html
SELECT last_name FROM employees
  WHERE salary IS NOT INFINITE;