-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
create or replace function split_part(string    varchar2, 
                                      delimiter varchar2,
                                      position  pls_integer)
          return varchar2 SQL_MACRO(Scalar) is
begin
  return q'{
    regexp_substr(replace(string, delimiter||delimiter, delimiter||' '||delimiter), 
                  '[^'||delimiter||']+', 1, position, 'imx')
  }';
end;
/
SELECT split_part( sysdate, '-', 2) month from dual;