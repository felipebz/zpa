-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/populating-json-data-memory-column-store.html
ALTER TABLE j_purchaseorder INMEMORY TEXT (po_document);