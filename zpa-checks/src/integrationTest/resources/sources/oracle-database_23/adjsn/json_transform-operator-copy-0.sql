-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-copy.html
SELECT json_transform('{"a":[], "b":[ {"x":1}, {"x":2}, {"x":3} ]}',
                      COPY '$.a' = PATH '$.b.x',
                      REMOVE '$.b');