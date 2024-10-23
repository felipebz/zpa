-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/creating-bitmap-indexes-json_value.html
CREATE BITMAP INDEX cost_ctr_idx ON j_purchaseorder
  (json_value(po_document, '$.CostCenter'));