-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-function-json_query.html
SELECT json_query(data, '$.ShippingInstructions.Phone[*].type'
                  WITH WRAPPER)
  FROM j_purchaseorder;