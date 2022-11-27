-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/NLS_LOWER.html
SELECT NLS_LOWER('NOKTASINDA', 'NLS_SORT = XTurkish') "Lowercase"
  FROM DUAL;