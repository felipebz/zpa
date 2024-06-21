-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/function-JSON_TABLE.html
SELECT *
  FROM j_purchaseorder NESTED
       po_document.ShippingInstructions.Phone[*]
         COLUMNS (type, "number")