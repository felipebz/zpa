-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-in-oracle-database.html
SELECT data FROM customers c
  ORDER BY c.data.revenue;