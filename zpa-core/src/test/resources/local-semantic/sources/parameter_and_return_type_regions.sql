create or replace package ov as procedure k(x number); procedure k(x date); procedure k(x varchar2); end;
/
create or replace package body ov as
  procedure k(x number) is begin say('NUMBER'); end;
  procedure k(x date) is begin say('DATE'); end;
  procedure k(x varchar2) is begin say('VARCHAR2'); end;
end;
/
create or replace package tp as type rec_t is record (f number, d date); subtype string is number; type rec2_t is record (s string); end;
/

create or replace procedure ret_a is
  subtype t is number;
  function f return t is subtype t is date; begin return sysdate; end;
begin
  ov.k(f);
end;
/
create or replace procedure ret_b is
  subtype t is number;
  procedure p(a t) is subtype t is date; begin ov.k(a); end;
begin
  p(1);
end;
/
create or replace procedure ret_d is
  subtype t is number;
  function f(a t) return number is subtype t is date; begin ov.k(a); return 0; end;
begin
  ov.k(f(1));
end;
/
create or replace package pkg_rg as type t is record (f number); procedure q(a t); function fn return t; procedure run; end;
/

create or replace package body pkg_rg as
  procedure q(a t) is begin null; end;
  function fn return t is type t is table of date index by pls_integer; begin return null; end;
  procedure run is begin null; end;
end;
/
create or replace procedure ret_e is
  subtype t is number;
  function f return t;
  function f return t is subtype t2 is date; begin return 1; end;
begin
  ov.k(f);
end;
/
create or replace procedure ret_f is
  subtype t is number;
  function f return t is v t := 1; subtype t is date; begin return v; end;
begin
  ov.k(f);
end;
/
create or replace procedure case_gc is begin ret_b; end;
/
create or replace procedure case_ge is begin ret_d; end;
/
create or replace procedure case_gb is begin ret_a; end;
/
create or replace procedure case_gf is begin ret_e; end;
/
