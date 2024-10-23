-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/error-clause-sql-query-functions-and-conditions.html
ALTER SESSION SET JSON_BEHAVIOR="ON_ERROR:ERROR"

SELECT json_value('[{a:1},{a:2}]', '$.a');