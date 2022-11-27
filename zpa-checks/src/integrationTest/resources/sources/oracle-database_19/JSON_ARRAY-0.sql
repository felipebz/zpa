-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/JSON_ARRAY.html
SELECT JSON_ARRAY (     
    JSON_OBJECT('percentage' VALUE .50),
    JSON_ARRAY(1,2,3),
    100,
    'California',
    null
    NULL ON NULL
    ) "JSON Array Example"
  FROM DUAL;