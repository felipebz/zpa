-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-right-hand-side-rhs-path-expressions.html
SELECT json_transform('{"a":[ 1,2 ], b:[ {c:3}, {c:4} ]}',
                      PREPEND '$.a' = PATH '$.b[*].c')