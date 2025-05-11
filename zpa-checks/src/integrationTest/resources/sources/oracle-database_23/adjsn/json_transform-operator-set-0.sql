-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-set.html
SELECT json_transform('{"a":1}',
                      SET '$.b' = 2);