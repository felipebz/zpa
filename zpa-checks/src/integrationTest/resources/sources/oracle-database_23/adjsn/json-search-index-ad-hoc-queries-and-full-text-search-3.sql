-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-search-index-ad-hoc-queries-and-full-text-search.html
CREATE SEARCH INDEX po_search_idx ON j_purchaseorder (po_document)
  FOR JSON PARAMETERS ('SYNC (ON COMMIT)');