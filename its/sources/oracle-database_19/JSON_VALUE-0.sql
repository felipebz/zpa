SELECT JSON_VALUE('{a:100}', '$.a') AS value
  FROM DUAL;