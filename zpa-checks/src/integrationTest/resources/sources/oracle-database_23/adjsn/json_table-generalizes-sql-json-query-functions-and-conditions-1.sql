-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_table-generalizes-sql-json-query-functions-and-conditions.html
SELECT jt.requestor, jt.phones
  FROM j_purchaseorder,
       json_table(data, '$'
         COLUMNS (
           requestor VARCHAR2(32 CHAR) PATH '$.Requestor',
           phones    VARCHAR2(100 CHAR) FORMAT JSON
                     PATH '$.ShippingInstructions.Phone',
           partial   BOOLEAN PATH '$.AllowPartialShipment',
           has_zip   BOOLEAN EXISTS
                     PATH '$.ShippingInstructions.Address.zipCode')) jt
  WHERE jt.partial AND jt.has_zip;