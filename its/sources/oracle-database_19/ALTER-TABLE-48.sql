ALTER TABLE list_customers 
   MERGE PARTITIONS asia, rest INTO PARTITION rest;