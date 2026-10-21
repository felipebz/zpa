create or replace procedure case_a is
  e exception;
  e number;
begin
  say('unreferenced dup exception/var');
end;
/
create or replace procedure case_b is
  e number := 1;
begin
  declare
    e exception;
  begin
    raise e;
  exception when e then say('inner exception e caught');
  end;
  say('outer e='||e);
end;
/
