-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
create or replace function greet(name varchar2 default 'World')
                  return varchar2 SQL_MACRO(Scalar) is
begin
  return q'{ 'Hello, ' || name || '!' }';
end;
/