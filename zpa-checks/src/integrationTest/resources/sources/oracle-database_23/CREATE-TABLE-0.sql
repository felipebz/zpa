-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
create or replace trigger t1_t 
before insert or update on t1 for each row
begin
  :new.c2 := NULL;
end;