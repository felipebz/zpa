-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/type-clause-sql-functions-and-conditions.html
SELECT json_exists('{"a" : {"b" : "42"}}',
                   '$.a?(@.b in (42, 314))');