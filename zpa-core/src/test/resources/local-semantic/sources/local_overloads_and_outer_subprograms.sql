create or replace package pk_n is
  procedure f(p number);
  procedure run;
end;
/
create or replace package body pk_n is
  procedure f(p number) is begin say('pk_n.f(number)'); end;
  procedure run is
    procedure f(p varchar2) is begin say('pk_n.run nested f(varchar2)'); end;
  begin
    f(1);
    pk_n.f(1);
  end;
end;
/
create or replace procedure case_a is
  procedure f(p number) is begin say('outer f(number)'); end;
begin
  declare
    procedure f(p date) is begin say('inner f(date)'); end;
  begin
    f(sysdate);
  end;
end;
/
create or replace procedure case_b is
  procedure f(p number) is begin say('outer f(number)'); end;
begin
  declare
    procedure f(p date) is begin say('inner f(date)'); end;
  begin
    f(1);
  end;
end;
/
create or replace procedure case_c is
  procedure f(p number) is begin say('outer f(number)'); end;
begin
  declare
    procedure f(p varchar2) is begin say('inner f(varchar2)'); end;
  begin
    f(1);
  end;
end;
/
create or replace procedure case_d is
begin
  pk_n.run;
end;
/
create or replace procedure case_e is
  procedure f(p number) is begin say('outer f(number)'); end;
begin
  declare
    f number := 1;
  begin
    f(1);
  end;
end;
/
create or replace procedure case_f is
  procedure f(p number) is begin say('outer f(number)'); end;
begin
  <<blk>>
  declare
    procedure f(p date) is begin say('inner f(date)'); end;
  begin
    case_f.f(1);
  end;
end;
/
