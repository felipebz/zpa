-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-intersect.html
SELECT json_transform('{"a":[ 1, 2, 3 ], "b":[ 2, 3, 4 ]}',
                      INTERSECT '$.a' = PATH '$.b[*]')