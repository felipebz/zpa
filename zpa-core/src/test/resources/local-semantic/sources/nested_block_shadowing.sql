create or replace procedure case_a is
  x number := 1;
begin
  declare
    x varchar2(10) := 'inner';
  begin
    say('inner x='||x);
    declare
      x date := date '2001-01-01';
    begin
      say('innermost x='||to_char(x,'YYYY'));
    end;
    say('back inner x='||x);
  end;
  say('outer x='||x);
end;
/
