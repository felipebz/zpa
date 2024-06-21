-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/populate-json-type-column-querying-textual-json.html
DROP TABLE j_purchaseorder;
ALTER TABLE j_purchaseorder_new RENAME TO j_purchaseorder;