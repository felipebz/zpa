begin
 null;
end;
create package body pkg is
  procedure foo is
  begin
    null;
  end;
end;
create procedure foo is
begin
  null;
end;
create function foo return number is
begin
  null;
end;
-- package specification should not be considered for duplication detection
create package pkg is
  procedure foo;
end;

create view foo as
select * from dual;

create trigger foo 
before insert on tab
begin 
  null;
end;
