-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
SELECT json_object(shipping RETURNING JSON)
  FROM po_ship;