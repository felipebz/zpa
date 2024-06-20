-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE list_customers MODIFY PARTITION asia 
   UNUSABLE LOCAL INDEXES;