-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/type-clause-sql-functions-and-conditions.html
SELECT json_query('{"a" : [ 314, "42", "alpha" ]}',
                  '$.a?(@ == 42)');