-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-prepend.html
SELECT json_transform('{"a": [ {"b": [ 1,2 ]},
                               {"b": [ 3,4 ]} ]}',
                      SET '$var' = json_array(5, 'cat'),
                      PREPEND '$.a[*].b' = PATH '$var[*]');