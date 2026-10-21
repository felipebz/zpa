create or replace procedure case_a is
begin
  <<Outer>>
  declare
    x number := 1;
  begin
    declare
      x number := 2;
    begin
      say('OUTER.x='||OUTER.x||' outer.x='||outer.x||' x='||x);
    end;
  end;
end;
/
create or replace procedure case_b is
begin
  <<lp>>
  for i in 1..2 loop
    declare
      i number := 99;
    begin
      say('lp.i='||lp.i||' i='||i);
    end;
  end loop;
end;
/
create or replace procedure case_c is
begin
  <<a>>
  declare
    x varchar2(10) := 'outer';
  begin
    <<a>>
    declare
      x varchar2(10) := 'inner';
    begin
      say('a.x='||a.x);
    end;
  end;
end;
/
create or replace procedure case_d is
begin
  <<b1>>
  declare
    x number := 1;
  begin
    null;
  end;
  <<b2>>
  begin
    say('b1.x='||b1.x);
  end;
end;
/
