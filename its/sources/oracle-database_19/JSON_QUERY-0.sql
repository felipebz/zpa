SELECT JSON_QUERY('{a:100, b:200, c:300}', '$') AS value
  FROM DUAL;