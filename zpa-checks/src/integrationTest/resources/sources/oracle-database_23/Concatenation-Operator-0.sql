-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Concatenation-Operator.html
SELECT 'Name is ' || last_name
  FROM employees
  ORDER BY last_name;