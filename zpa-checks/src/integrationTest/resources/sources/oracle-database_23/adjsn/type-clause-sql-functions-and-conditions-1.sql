-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/type-clause-sql-functions-and-conditions.html
SELECT json_value('{"a" : "42"}', '$.a' RETURNING NUMBER);