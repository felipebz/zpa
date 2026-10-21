create or replace procedure case_a is
  a number := b;
  b number := 1;
begin
  say('a='||a);
end;
/
create or replace procedure case_b is
  b number := 100;
begin
  declare
    a number := b;
    b number := 1;
  begin
    say('a='||a||' b='||b);
  end;
end;
/
create or replace procedure case_c is
  b number := 100;
begin
  declare
    a number := b;
  begin
    say('a='||a);
  end;
end;
/
