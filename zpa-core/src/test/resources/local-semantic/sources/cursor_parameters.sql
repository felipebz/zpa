create or replace procedure case_a is
  x number := 99;
  v number;
  cursor c(x number) is select x as val from dual;
begin
  open c(1); fetch c into v; close c;
  say('cursor returned '||v||', outer x='||x);
end;
/
create or replace procedure case_b is
  v varchar2(10);
  cursor c(dummy varchar2) is select dummy from dual where dummy = dummy;
begin
  open c('Z'); fetch c into v; close c;
  say('cursor returned '||v);
end;
/
create or replace procedure case_c is
  cursor c(cp number) is select cp from dual;
begin
  say('cp='||cp);
end;
/
create or replace procedure case_d is
  v number;
  cursor c(a number default 5) is select a from dual;
begin
  open c; fetch c into v; close c;
  say('default a='||v);
  for r in c(7) loop say('for r.a='||r.a); end loop;
end;
/
