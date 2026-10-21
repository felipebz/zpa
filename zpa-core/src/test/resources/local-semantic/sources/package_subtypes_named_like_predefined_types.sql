create or replace package pkg_p_integer is
  subtype integer is date;
  g integer := date '2020-02-02';
end;
/
create or replace package pkg_p_date is
  subtype date is number;
end;
/
create or replace package pkg_p_varchar2 is
  subtype varchar2 is number;
end;
/
create or replace package pkg_p_number is
  subtype number is date;
end;
/
create or replace package pkg_p_pls is
  subtype pls_integer is date;
end;
/
create or replace package pkg_p_boolean is
  subtype boolean is number;
end;
/
create or replace package pkg_p_string is
  subtype string is number;
end;
/
create or replace package pkg_p_qdate is
  subtype "DATE" is number;
end;
/
create or replace package pkg_p_qnumber is
  subtype "NUMBER" is date;
  g "NUMBER";
  h number;
end;
/
create or replace procedure case_a is
  v pkg_p_integer.integer := sysdate;
  w integer := 5;
begin
  say('pkg g='||to_char(pkg_p_integer.g,'YYYY-MM-DD')||' v(year)='||to_char(v,'YYYY')||' w='||w);
end;
/
