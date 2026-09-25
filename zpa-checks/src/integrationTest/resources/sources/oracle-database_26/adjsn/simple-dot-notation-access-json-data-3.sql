-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/simple-dot-notation-access-json-data.html
SELECT t.data.name.string() FROM fruit t;