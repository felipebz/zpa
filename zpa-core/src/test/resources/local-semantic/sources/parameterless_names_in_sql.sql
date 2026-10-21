create table tab_n(sysdate_c date, n number);
insert into tab_n values (date '1999-09-09', 1);
create or replace package pkg_date is
  sysdate date := date '1990-01-01';
end;
/
create or replace procedure case_a is
  d date;
  function sysdate return date is begin return date '1980-01-01'; end;
begin
  say('plsql sysdate='||to_char(sysdate,'YYYY'));
  select sysdate into d from dual;
  say('SQL sysdate='||to_char(d,'YYYY'));
end;
/
create or replace procedure case_b is
  d date;
begin
  select sysdate into d from dual;
  say('no local: SQL sysdate year='||to_char(d,'YYYY')||' pkg_date.sysdate='||to_char(pkg_date.sysdate,'YYYY'));
end;
/
create or replace procedure case_c is
  n number := 42;
  m number;
begin
  select n into m from tab_n;
  say('column n wins in select list: '||m);
end;
/
