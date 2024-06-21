-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/indexes-for-json-data.html
CREATE BITMAP INDEX cost_ctr_idx ON j_purchaseorder
  (json_value(po_document, '$.CostCenter'));