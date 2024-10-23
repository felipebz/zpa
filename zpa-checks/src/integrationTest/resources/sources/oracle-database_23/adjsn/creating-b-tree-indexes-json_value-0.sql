-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/creating-b-tree-indexes-json_value.html
CREATE UNIQUE INDEX po_num_idx1 ON j_purchaseorder po
  (po.po_document.PONumber.number());