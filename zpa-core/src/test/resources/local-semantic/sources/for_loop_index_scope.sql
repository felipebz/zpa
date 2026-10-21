create or replace procedure case_a is
  i varchar2(10) := 'outer';
begin
  for i in 1..2 loop
    say('loop i+1='||(i + 1));
  end loop;
  say('after loop i='||i);
end;
/
create or replace procedure case_b is
begin
  for j in 1..2 loop null; end loop;
  say('j='||j);
end;
/
create or replace procedure case_c is
begin
  for i in 1..2 loop
    i := 5;
  end loop;
end;
/
create or replace procedure case_d is
begin
  <<o>>
  for r in (select 1 a from dual) loop
    for r in (select 2 a from dual) loop
      say('o.r.a='||o.r.a||' r.a='||r.a);
    end loop;
  end loop;
end;
/
create or replace procedure case_e is
  r number := 42;
begin
  for r in (select 'rec' a from dual) loop
    say('r.a='||r.a);
  end loop;
  say('after r='||r);
end;
/
create or replace procedure case_f is
  i number := 10;
begin
  for k in 1..i loop
    null;
  end loop;
  for i in i..i+1 loop
    say('bounds use outer i; i='||i);
  end loop;
end;
/
