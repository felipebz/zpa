-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/function-JSON_QUERY.html
SELECT json_query(po_document, '$.ShippingInstructions.Phone[*].type'
                  WITH WRAPPER)
  FROM j_purchaseorder;