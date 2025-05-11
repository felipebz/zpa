-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-right-hand-side-rhs-path-expressions.html
SELECT json_transform('{"a":[ 1,2,3 ]}',
                      SET '$.b' = PATH '$.a[*].sum()');