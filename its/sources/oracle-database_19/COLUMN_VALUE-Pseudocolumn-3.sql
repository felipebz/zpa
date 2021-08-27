-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/COLUMN_VALUE-Pseudocolumn.html
SELECT t.COLUMN_VALUE
  FROM TABLE(phone_list(phone(1,2,3))) p, TABLE(p.COLUMN_VALUE) t;