-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/ALTER-TYPE-statement.html
type     My_ADT authid Definer is object(a1 number)

select Version#||Chr(10)||Text t
from DBA_Type_Versions
where Owner = 'USR'
and Type_Name = 'MY_ADT'
/