-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/COLUMN_VALUE-Pseudocolumn.html
SELECT t.COLUMN_VALUE
  FROM TABLE(phone(1,2,3)) t;