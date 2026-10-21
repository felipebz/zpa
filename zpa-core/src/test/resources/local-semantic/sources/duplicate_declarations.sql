create or replace procedure case_a is
  x number;
  x varchar2(10);
begin
  say('dup unreferenced');
end;
/
create or replace procedure case_b is
  x number;
  x varchar2(10);
begin
  x := 1;
end;
/
create or replace procedure case_c is
  x number;
  procedure x is begin say('proc x'); end;
begin
  say('var+proc same name unreferenced');
end;
/
create or replace procedure case_d is
  x number;
  procedure x(a number) is begin say('proc x'); end;
begin
  x(1);
end;
/
create or replace procedure case_e is
begin
  <<x>>
  declare
    x number := 5;
  begin
    say('x.x='||x.x||' x='||x);
  end;
end;
/
