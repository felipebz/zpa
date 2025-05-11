-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator.html
SELECT json_transform('{"a":[ 1, 2, 3, 4 ], "b":[ 2, 5 ]}',
                      MINUS '$.a' = PATH '$.b[*]')