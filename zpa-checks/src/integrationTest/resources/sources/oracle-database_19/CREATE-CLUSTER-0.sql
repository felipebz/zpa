-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-CLUSTER.html
CREATE CLUSTER personnel
   (department NUMBER(4))
SIZE 512 
STORAGE (initial 100K next 50K);