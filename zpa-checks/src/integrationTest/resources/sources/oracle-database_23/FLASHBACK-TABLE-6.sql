-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/FLASHBACK-TABLE.html
FLASHBACK TABLE employees_test
  TO TIMESTAMP (SYSTIMESTAMP - INTERVAL '1' minute);