-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/oracle-sql-function-json_mergepatch.html
SELECT json_mergepatch(data, '{"Special Instructions":null}'
                       RETURNING CLOB PRETTY)
  FROM j_purchaseorder;