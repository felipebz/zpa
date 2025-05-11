-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/indexing-multiple-json-fields-using-composite-b-tree-index.html
SELECT data FROM j_purchaseorder
  WHERE json_value(data, '$.User')       = 'ABULL'
    AND json_value(data, '$.CostCenter') = 'A50';