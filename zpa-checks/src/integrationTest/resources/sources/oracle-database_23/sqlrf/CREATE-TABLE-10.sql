-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
CREATE TABLE j_purchaseorder
        (id VARCHAR2 (32) NOT NULL PRIMARY KEY,
         date_loaded TIMESTAMP (6) WITH TIME ZONE,
         po_document JSON );