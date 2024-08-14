-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/indexes-for-json-data.html
CREATE SEARCH INDEX po_search_idx ON j_purchaseorder (po_document)
  FOR JSON PARAMETERS ('MAINTENANCE AUTO');