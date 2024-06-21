-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-path-expressions.html
SELECT avg(json_value(po_document, 
                      '$.LineItems[*].Quantity.avg()'))
  FROM j_purchaseorder;