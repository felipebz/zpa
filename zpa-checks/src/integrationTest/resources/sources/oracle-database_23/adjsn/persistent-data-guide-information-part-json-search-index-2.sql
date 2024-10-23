-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/persistent-data-guide-information-part-json-search-index.html
ALTER INDEX po_search_idx REBUILD PARAMETERS ('DATAGUIDE ON');