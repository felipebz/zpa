-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-condition-json_exists.html
SELECT json_exists('{a : null}', '$.a');