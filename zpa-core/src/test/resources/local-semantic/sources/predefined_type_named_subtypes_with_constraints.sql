create or replace package pkg_p_varchar2 is
  subtype varchar2 is number;
  g varchar2(10) := 'abc';
end;
/
create or replace procedure case_a is
  subtype varchar2 is number;
  v varchar2(10) := 'abc';
begin
  say('v='||v);
end;
/
create or replace procedure case_b is
  subtype number is date;
  v number(5) := 5;
begin
  say('v='||v);
end;
/
create or replace procedure case_c is
  subtype varchar2 is number;
  v varchar2(3) := 12345;
begin
  say('v='||v);
end;
/
create or replace procedure case_d is
begin
  say('pkg_p_varchar2.g='||pkg_p_varchar2.g);
end;
/
create or replace procedure case_e is
  subtype integer is date;
  v integer(3) := 5;
begin
  say('v='||v);
end;
/
