-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-union.html
SELECT json_transform('{"a":[ 1, 2, 3 ], "b":[ 2, 5, 3, 4 ]}',
                      UNION '$.a' = PATH '$.b')