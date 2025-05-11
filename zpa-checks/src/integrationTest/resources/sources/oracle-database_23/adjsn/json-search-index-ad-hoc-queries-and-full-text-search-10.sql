-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-search-index-ad-hoc-queries-and-full-text-search.html
SELECT data FROM j_purchaseorder
  WHERE json_value(data, '$.User') = 'AKHOO';