-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/persistent-data-guide-information-part-json-search-index.html
CREATE SEARCH INDEX po_dg_only_idx
  ON j_purchaseorder (data) FOR JSON
    PARAMETERS ('DATAGUIDE ON SEARCH_ON NONE');