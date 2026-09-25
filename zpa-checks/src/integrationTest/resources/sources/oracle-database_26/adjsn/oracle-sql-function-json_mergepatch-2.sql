-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/oracle-sql-function-json_mergepatch.html
UPDATE j_purchaseorder SET data =
  json_mergepatch(data, '{"Special Instructions":null}');