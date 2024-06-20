-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE list_customers 
   MERGE PARTITIONS asia, rest INTO PARTITION rest;