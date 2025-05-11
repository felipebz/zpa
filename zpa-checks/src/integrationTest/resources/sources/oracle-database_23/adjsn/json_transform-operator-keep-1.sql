-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-keep.html
SELECT json_transform('{"a":1, "b":2, "c":3, "d":4}',
                      KEEP '$.*?(@ > 2)')