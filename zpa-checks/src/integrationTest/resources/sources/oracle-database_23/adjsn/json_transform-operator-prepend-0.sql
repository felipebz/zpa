-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-prepend.html
SELECT json_transform('{"a":[1,2]}',
                      INSERT '$.a' = '0')