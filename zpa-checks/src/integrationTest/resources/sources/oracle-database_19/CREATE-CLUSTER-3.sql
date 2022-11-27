-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-CLUSTER.html
CREATE CLUSTER language (cust_language VARCHAR2(3))
   SIZE 512 HASHKEYS 10
   STORAGE (INITIAL 100k next 50k);