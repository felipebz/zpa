-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CHARTOROWID.html
SELECT last_name
  FROM employees
  WHERE ROWID = CHARTOROWID('AAAFd1AAFAAAABSAA/');