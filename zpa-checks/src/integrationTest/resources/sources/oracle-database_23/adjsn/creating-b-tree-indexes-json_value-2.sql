-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/creating-b-tree-indexes-json_value.html
CREATE UNIQUE INDEX po_num_idx2 ON j_purchaseorder
  (json_value(po_document, '$.PONumber.number()' 
              ERROR ON ERROR));