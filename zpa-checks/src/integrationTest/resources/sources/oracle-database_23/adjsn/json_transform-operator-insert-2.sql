-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-insert.html
SELECT json_transform('{"a":[1,2,3,4]}',
                      INSERT '$.a[2]' = 'hello')