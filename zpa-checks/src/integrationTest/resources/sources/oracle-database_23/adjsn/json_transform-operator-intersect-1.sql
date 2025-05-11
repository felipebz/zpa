-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-intersect.html
SELECT json_transform('{"a":[ 1,2 ], b:[ {c:3}, {c:4} ]}',
                      PREPEND '$.a' = PATH '$.b[*].c')