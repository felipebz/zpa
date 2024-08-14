-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/indexes-for-json-data.html
SELECT po_document FROM j_purchaseorder
  WHERE json_value(po_document, '$.User') = 'AKHOO';