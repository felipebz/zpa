-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-sql-json-function-json_value-boolean-json-value.html
SELECT json_value(po_document, '$.AllowPartialShipment')
  FROM j_purchaseorder;