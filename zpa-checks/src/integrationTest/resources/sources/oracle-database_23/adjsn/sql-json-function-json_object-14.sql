-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-function-json_object.html
CREATE TABLE po_ship
  AS SELECT json_value(data, '$.ShippingInstructions'
                       RETURNING shipping_t)
  shipping
  FROM j_purchaseorder;
DESCRIBE po_ship;