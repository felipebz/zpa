create or replace procedure case_a is
  i number := 10;
begin
  for i in i..i+1 loop
    say('i='||i);
  end loop;
end;
/
create or replace procedure case_b is
begin
  for i in 1..i loop
    say('i='||i);
  end loop;
end;
/
create or replace procedure case_c is
  i number := 2;
begin
  for i in 1..i loop
    say('i='||i);
  end loop;
end;
/
create or replace procedure case_d is
  i number := 2;
begin
  for r in (select level lv from dual connect by level <= i) loop
    say('lv='||r.lv);
  end loop;
end;
/
