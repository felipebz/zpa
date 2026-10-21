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

create or replace procedure rv(f date) is
  rv tp.rec_t;
begin
  rv.f := 1;
  ov.k(rv.f);
end;
/
create or replace procedure case_a is begin rv(sysdate); end;
/
