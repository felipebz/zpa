-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/data-type-considerations-json_value-indexing-and-querying.html
SELECT count(*) FROM j_purchaseorder po
  WHERE to_number(json_value(data, '$.PONumber')) > 1500;