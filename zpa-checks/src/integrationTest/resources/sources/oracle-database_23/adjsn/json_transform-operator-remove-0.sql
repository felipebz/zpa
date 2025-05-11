-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-remove.html
SELECT json_transform('{"a":[0,1,2]}',
                      REMOVE '$.a[0]')