-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-data-type.html
SELECT data FROM customers c
  ORDER BY c.data.revenue;