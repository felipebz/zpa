-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/clauses-used-in-functions-and-conditions-for-json.html
SELECT json_value('[{a:1},{a:2}]', '$.a');