-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/loading-external-json-data.html
DROP TABLE j_purchaseorder;
CREATE TABLE j_purchaseorder
  (id          VARCHAR2 (32) NOT NULL PRIMARY KEY,
   date_loaded TIMESTAMP (6) WITH TIME ZONE,
   data        JSON);