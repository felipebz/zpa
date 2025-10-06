-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/handling-input-values-sql-json-generation-functions.html
SELECT json_array(getX(5) FORMAT JSON);