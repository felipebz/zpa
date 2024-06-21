-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/oracle-sql-function-json_transform.html
UPDATE j_purchaseorder SET po_document =
  json_transform(po_document, SET '$.lastUpdated' = SYSTIMESTAMP);