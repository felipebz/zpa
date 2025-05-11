-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-replace.html
SELECT json_transform('{"a":1}',
                      REPLACE '$.a' = 2,
                      REPLACE '$.b' = 3);