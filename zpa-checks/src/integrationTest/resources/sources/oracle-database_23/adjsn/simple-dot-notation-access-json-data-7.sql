-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/simple-dot-notation-access-json-data.html
SELECT po.data.ShippingInstructions.Phone
  FROM j_purchaseorder po;
SELECT json_query(data, '$.ShippingInstructions.Phone')
  FROM j_purchaseorder;