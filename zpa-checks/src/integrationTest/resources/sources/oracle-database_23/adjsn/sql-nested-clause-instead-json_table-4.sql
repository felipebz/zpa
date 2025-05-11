-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-nested-clause-instead-json_table.html
SELECT *
  FROM j_purchaseorder NESTED
       data.ShippingInstructions.Phone[*]
         COLUMNS (type, "number")