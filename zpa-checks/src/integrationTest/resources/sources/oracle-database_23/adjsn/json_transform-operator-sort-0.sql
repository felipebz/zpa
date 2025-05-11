-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-sort.html
SELECT json_transform('{"a":[ 1, null, 2, "cat", true, 3.1416 ]}',
                      SORT '$.a' DESC);