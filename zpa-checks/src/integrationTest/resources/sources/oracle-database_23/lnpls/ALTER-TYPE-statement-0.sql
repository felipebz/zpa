-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/ALTER-TYPE-statement.html
SELECT Version# 
FROM DBA_TYPE_VERSIONS
WHERE Owner = schema
AND Name = 'type_name'
AND Type = 'TYPE'