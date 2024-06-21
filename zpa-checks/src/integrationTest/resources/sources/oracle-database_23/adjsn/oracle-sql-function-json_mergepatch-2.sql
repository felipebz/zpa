-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/oracle-sql-function-json_mergepatch.html
UPDATE j_purchaseorder SET po_document =
  json_mergepatch(po_document, '{"Special Instructions":null}');