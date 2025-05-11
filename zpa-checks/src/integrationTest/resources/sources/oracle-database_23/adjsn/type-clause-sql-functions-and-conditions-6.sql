-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/type-clause-sql-functions-and-conditions.html
SELECT json_transform('{"a" : "42"}',
                      INSERT '$.b' = PATH '$?(@.a > 0).a + 1'
                      TYPE(LAX));