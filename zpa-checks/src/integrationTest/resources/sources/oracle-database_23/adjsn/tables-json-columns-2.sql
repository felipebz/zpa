-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/tables-json-columns.html
CREATE TABLE j_purchaseorder
  (id          VARCHAR2 (32) NOT NULL PRIMARY KEY,
   date_loaded TIMESTAMP (6) WITH TIME ZONE,
   po_document VARCHAR2 (23767)
   CONSTRAINT ensure_json CHECK (po_document is json));