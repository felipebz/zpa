-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Floating-Point-Conditions.html
SELECT COUNT(*) FROM employees
  WHERE commission_pct IS NOT NAN;