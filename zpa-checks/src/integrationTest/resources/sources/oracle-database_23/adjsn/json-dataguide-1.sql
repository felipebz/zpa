-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-dataguide.html
CREATE SEARCH INDEX po_dg_only_idx
  ON j_purchaseorder (po_document) FOR JSON
    PARAMETERS ('DATAGUIDE ON SEARCH_ON NONE');