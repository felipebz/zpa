-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/creating-b-tree-indexes-json_value.html
CREATE UNIQUE INDEX po_ref_idx1 ON j_purchaseorder
  (json_value(data, '$.Reference'
              RETURNING VARCHAR2(200) ERROR ON ERROR
              NULL ON EMPTY));