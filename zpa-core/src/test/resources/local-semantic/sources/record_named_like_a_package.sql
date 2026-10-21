create or replace package px is
  x varchar2(10) := 'pkg.x';
  y varchar2(10) := 'pkg.y';
end;
/
create or replace procedure case_a is
  type r is record(x varchar2(10));
  px r;
begin
  px.x := 'rec.x';
  say('px.x='||px.x);
end;
/
create or replace procedure case_b is
  type r is record(x varchar2(10));
  px r;
begin
  say('px.y='||px.y);
end;
/
create or replace procedure case_c is
begin
  declare
    px number := 1;
  begin
    null;
  end;
  say('after inner block px.y='||px.y);
end;
/
