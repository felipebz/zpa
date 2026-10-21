create or replace package pkg_p_date is
  subtype date is number;
  g date := 5;
end;
/
create or replace package pkg_p_varchar2 is
  subtype varchar2 is number;
  g varchar2(10) := 'abc';
end;
/
create or replace package pkg_p_varchar2b is
  subtype varchar2 is number;
  g varchar2 := 7;
end;
/
create or replace package pkg_p_number is
  subtype number is date;
  g number := date '2004-04-04';
end;
/
create or replace package pkg_p_qnumber is
  subtype "NUMBER" is date;
  h number := date '2005-05-05';
end;
/
create or replace procedure case_a is
  a pkg_p_date.date := 6;
  b pkg_p_varchar2b.varchar2 := 8;
  c pkg_p_number.number := date '2006-06-06';
  d date := date '2007-07-07';
  e number := 9;
begin
  say('a='||a||' pkgdate.g='||pkg_p_date.g||' b='||b||' pkg_vc.g='||pkg_p_varchar2b.g||' c='||to_char(c,'YYYY')||' pkgnum.g='||to_char(pkg_p_number.g,'YYYY')||' qnum.h='||to_char(pkg_p_qnumber.h,'YYYY')||' d='||to_char(d,'YYYY')||' e='||e);
end;
/
create or replace procedure case_b is
  subtype number is date;
  v number := date '2008-08-08';
  w standard.number := 3;
begin
  say('v='||to_char(v,'YYYY')||' w='||w);
end;
/
create or replace procedure case_c is
  subtype date is number;
  v date := 5;
begin
  say('v='||v);
end;
/
create or replace procedure case_d is
  subtype varchar2 is number;
  v varchar2 := 11;
begin
  say('v='||v);
end;
/
create or replace procedure case_e is
  subtype number is date;
  procedure f(p number) is begin say('f(p number) got year '||to_char(p,'YYYY')); end;
begin
  f(sysdate);
end;
/
create or replace procedure case_f is
  subtype number is date;
  v number := 5;
begin
  say('v='||v);
end;
/
create or replace procedure case_g is
  v standard.date := date '2009-09-09';
  w standard.varchar2(5) := 'ab';
begin
  say('v='||to_char(v,'YYYY')||' w='||w);
end;
/
