-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Literals.html
SELECT COUNT(*) 
  FROM employees 
  WHERE salary < BINARY_DOUBLE_INFINITY;