-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-INDEX.html
CREATE INDEX cust_last_name_ix ON customers (cust_last_name)
  GLOBAL PARTITION BY HASH (cust_last_name)
  PARTITIONS 4;