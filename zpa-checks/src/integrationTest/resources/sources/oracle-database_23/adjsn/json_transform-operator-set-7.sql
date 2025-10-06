-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-set.html
SELECT json_transform('{"a":[1,2,3]}',
                      SET '$.a[6]' = 5);