create or replace package pkg_b is
  x varchar2(10) := 'pkg.x';
end;
/
create or replace procedure case_a is
begin
  say('before block pkg_b.x='||pkg_b.x);
  <<pkg_b>>
  declare
    x varchar2(10) := 'label.x';
  begin
    null;
  end;
end;
/
create or replace procedure case_b is
  type r_t is record(x varchar2(10));
  b r_t;
begin
  b.x := 'rec b.x';
  declare
    z number;
  begin
    <<b>>
    declare
      x varchar2(10) := 'label b.x';
    begin
      null;
    end;
    say('after labeled block b.x='||b.x);
  end;
end;
/
create or replace procedure case_c is
  type r_t is record(x varchar2(10));
  b r_t;
begin
  b.x := 'rec b.x';
  declare
    z number;
  begin
    say('before labeled block b.x='||b.x);
    <<b>>
    declare
      x varchar2(10) := 'label b.x';
    begin
      null;
    end;
  end;
end;
/
create or replace procedure case_d is
begin
  <<lab>>
  declare
    x number := 1;
  begin
    null;
  end;
  <<lab>>
  declare
    x number := 2;
  begin
    say('second lab.x='||lab.x);
  end;
end;
/
create or replace procedure case_e is
begin
  <<lab>>
  declare
    x number := 1;
  begin
    null;
  end;
  <<lab>>
  declare
    x number := 2;
  begin
    null;
  end;
  say('two sibling labels lab, no ref');
end;
/
