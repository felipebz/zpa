-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-TABLE.html
ALTER TABLE list_customers MODIFY PARTITION asia 
   UNUSABLE LOCAL INDEXES;