declare
  a number;
begin
  a := 1;
  
  for i in 1..2 loop
    null;
  end loop;
end;
/
create procedure foo(x number) is
  cursor cur(c number) is
    select 1 from dual;
begin
  open cur;
end;
/
create function bar(x number) return number is
begin
  null;
end;
/
create package pkg as
  var number;
end;
/
create package body pkg as
  var number;
end;
/