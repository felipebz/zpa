-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/function-JSON_TABLE.html
SELECT id, requestor, type, "number"
  FROM j_purchaseorder NESTED
       po_document
         COLUMNS (Requestor,
                  NESTED ShippingInstructions.Phone[*]
                    COLUMNS (type, "number"));