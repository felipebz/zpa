-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/simple-dot-notation-access-json-data.html
SELECT po.po_document.ShippingInstructions.Phone.type
  FROM j_purchaseorder po;
SELECT json_query(po_document, '$.ShippingInstructions.Phone.type'
                  WITH WRAPPER)
  FROM j_purchaseorder;