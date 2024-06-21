-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
CREATE TABLE po_ship
  AS SELECT json_value(po_document, '$.ShippingInstructions'
                       RETURNING shipping_t)
  shipping
  FROM j_purchaseorder;
DESCRIBE po_ship;