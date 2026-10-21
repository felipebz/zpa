create or replace package pkg_a is
  x varchar2(10) := 'pkg.x';
  y varchar2(10) := 'pkg.y';
end;
/
create or replace procedure case_a is
begin
  <<pkg_a>>
  declare
    x varchar2(10) := 'label.x';
  begin
    say('pkg_a.x='||pkg_a.x);
  end;
end;
/
create or replace procedure case_b is
begin
  <<pkg_a>>
  declare
    x varchar2(10) := 'label.x';
  begin
    say('pkg_a.y='||pkg_a.y);
  end;
end;
/
create or replace procedure case_c is
begin
  <<pkg_a>>
  declare
    x varchar2(10) := 'label.x';
  begin
    null;
  end;
  say('after block pkg_a.x='||pkg_a.x);
end;
/
