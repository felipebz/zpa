-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_table-generalizes-sql-json-query-functions-and-conditions.html
SELECT json_value(po_document, '$.Requestor' RETURNING VARCHAR2(32)),
       json_query(po_document, '$.ShippingInstructions.Phone'
                  RETURNING VARCHAR2(100))
  FROM j_purchaseorder
  WHERE json_exists(po_document, '$.ShippingInstructions.Address.zipCode')
    AND json_value(po_document,  '$.AllowPartialShipment'
                   RETURNING BOOLEAN) = TRUE;