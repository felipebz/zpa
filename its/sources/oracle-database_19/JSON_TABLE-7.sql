SELECT *
FROM JSON_TABLE('[1,2,["a","b"]]', '$'
COLUMNS (outer_value_0 NUMBER PATH '$[0]',
         outer_value_1 NUMBER PATH '$[1]', 
         outer_value_2 VARCHAR2(20) FORMAT JSON PATH '$[2]'));