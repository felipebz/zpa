-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/type-clause-sql-functions-and-conditions.html
SELECT jt.* FROM json_table('{"a" : 314, "b" : "42"}',
                  '$' COLUMNS (a NUMBER PATH '$.a',
                               b NUMBER PATH '$.b')) jt;