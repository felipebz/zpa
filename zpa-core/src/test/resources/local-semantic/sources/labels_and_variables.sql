create or replace procedure case_a is
  type r_t is record(x varchar2(10));
  b r_t;
begin
  b.x := 'rec b.x';
  <<b>>
  declare
    x varchar2(10) := 'label b.x';
  begin
    say('b.x='||b.x);
  end;
end;
/
create or replace procedure case_b is
  type r_t is record(x varchar2(10));
  b r_t;
begin
  b.x := 'rec b.x';
  <<b>>
  declare
    y varchar2(10) := 'label b.y';
  begin
    say('b.x='||b.x);
  end;
end;
/
