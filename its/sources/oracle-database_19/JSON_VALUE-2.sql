SELECT JSON_VALUE('{a:{b:100}}', '$.a.b') AS value
  FROM DUAL;