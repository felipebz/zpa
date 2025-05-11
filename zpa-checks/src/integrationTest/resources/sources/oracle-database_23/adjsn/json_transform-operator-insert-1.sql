-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-insert.html
SELECT json_transform('{"a":1}',
                      INSERT '$.a' = 'hello')