-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/error-clause-sql-query-functions-and-conditions.html
SELECT json_value('[{a:1},{a:2}]', '$.a');