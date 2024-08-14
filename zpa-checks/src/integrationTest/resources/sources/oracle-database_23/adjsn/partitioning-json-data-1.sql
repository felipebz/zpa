-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/partitioning-json-data.html
SELECT DATA FROM orders p
  WHERE p.data.PONumber.number() = 1234;