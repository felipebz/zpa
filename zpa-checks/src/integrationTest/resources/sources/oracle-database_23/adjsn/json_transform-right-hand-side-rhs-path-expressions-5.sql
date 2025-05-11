-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-right-hand-side-rhs-path-expressions.html
SELECT json_transform('{travel:[ {"name":"Jack", "approval":[ 2023, 2024 ]},
                                 {"name":"Jill", "approval":[ 2024 ]} ]}',
                      SET '$var' = JSON('[ 2025,2026 ]'),
                      APPEND '$.travel.approval' = PATH '$var[*]');