-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/function-JSON_VALUE.html
SELECT json_value(po_document, '$.AllowPartialShipment'
                  RETURNING BOOLEAN)
  FROM j_purchaseorder;