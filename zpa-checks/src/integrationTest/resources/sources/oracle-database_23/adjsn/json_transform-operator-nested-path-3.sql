-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-nested-path.html
SELECT json_transform(
         data, 
         NESTED PATH '$.LineItems[*]'
           (SET '@.LineItemTotal' = PATH '@.Part.UnitPrice * @.Quantity'),
         SET '$.OrderTotal' = PATH '$.LineItems[*].LineItemTotal.sum()',
         REMOVE '$.LineItems[*].LineItemTotal')
  FROM j_purchaseorder;