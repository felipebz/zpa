create or replace procedure case_a is
  v varchar2(10);
  function upper(s varchar2) return varchar2 is begin return 'local'; end;
begin
  say('upper(a)='||upper('a')||' standard.upper(a)='||standard.upper('a'));
  select upper('a') into v from dual;
  say('SQL upper(a)='||v);
end;
/
create or replace procedure case_b is
  sysdate date := date '2000-01-01';
  d date;
begin
  say('plsql sysdate year='||to_char(sysdate,'YYYY'));
  select sysdate into d from dual;
  say('SQL sysdate year='||to_char(d,'YYYY'));
end;
/
create or replace procedure case_c is
  user varchar2(10) := 'me';
  u varchar2(128);
begin
  select user into u from dual;
  say('plsql user='||user||' SQL user is me? '||case when u = 'me' then 'Y' else 'N' end);
end;
/
create or replace function fx return varchar2 is begin return 'standalone fx'; end;
/
create or replace procedure case_d is
  fx varchar2(10) := 'var fx';
begin
  say('fx='||fx);
end;
/
