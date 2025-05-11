-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_table-generalizes-sql-json-query-functions-and-conditions.html
SELECT json_value(data, '$.Requestor' RETURNING VARCHAR2(32)),
       json_query(data, '$.ShippingInstructions.Phone'
                  RETURNING VARCHAR2(100))
  FROM j_purchaseorder
  WHERE json_exists(data, '$.ShippingInstructions.Address.zipCode')
    AND json_value(data,  '$.AllowPartialShipment'
                   RETURNING BOOLEAN) = TRUE;