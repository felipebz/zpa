-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/indexing-multiple-json-fields-using-composite-b-tree-index.html
SELECT po_document FROM j_purchaseorder
  WHERE json_value(po_document, '$.User')       = 'ABULL'
    AND json_value(po_document, '$.CostCenter') = 'A50';