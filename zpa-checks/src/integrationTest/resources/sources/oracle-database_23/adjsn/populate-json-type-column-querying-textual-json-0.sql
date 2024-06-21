-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/populate-json-type-column-querying-textual-json.html
CREATE TABLE j_purchaseorder_new PARALLEL NOLOGGING AS
SELECT id id, date_loaded date_loaded, JSON(po_document) po_document
  FROM j_purchaseorder;