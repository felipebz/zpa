-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Literals.html
SELECT COUNT(*) 
  FROM employees 
  WHERE TO_BINARY_FLOAT(commission_pct)
     != BINARY_FLOAT_NAN;