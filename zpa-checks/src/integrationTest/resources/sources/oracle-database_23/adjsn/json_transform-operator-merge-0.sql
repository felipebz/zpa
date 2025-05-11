-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-merge.html
SELECT json_transform('{"a":{"x":1, "y":2}, "b":{"y":3, "z":4}}',
                      MERGE '$.a' = PATH '$.b')