-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-nested-clause-instead-json_table.html
SELECT id, requestor, type, "number"
  FROM j_purchaseorder NESTED
       data
         COLUMNS (Requestor,
                  NESTED ShippingInstructions.Phone[*]
                    COLUMNS (type, "number"));