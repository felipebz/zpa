-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/sql-json-function-json_object.html
SELECT json_object(shipping RETURNING JSON)
  FROM po_ship;