-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-path-expression-item-methods.html
SELECT json_value(data, 
                  '$.LineItems[*].Quantity.avg()')
  FROM j_purchaseorder;