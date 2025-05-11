-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-append.html
SELECT json_transform('{"a":[1,2,3]}',
                      INSERT '$.a[last+1]' = 'hello');