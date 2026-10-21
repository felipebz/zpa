create or replace procedure case_a is
  function f return varchar2 is begin return 'func f'; end;
begin
  say('f='||f||' f()='||f());
  declare
    f varchar2(10) := 'var f';
  begin
    say('inner f='||f);
  end;
end;
/
create or replace procedure case_b is
  f varchar2(10) := 'var f';
begin
  declare
    function f return varchar2 is begin return 'inner func f'; end;
  begin
    say('inner f='||f);
  end;
  say('outer f='||f);
end;
/
