create or replace procedure case_a is
  procedure nb is begin say('outer nb'); end;
begin
  declare
    procedure a is begin nb; end;
    procedure nb is begin say('inner nb'); end;
  begin
    a;
  end;
end;
/
create or replace procedure case_b is
  procedure nb is begin say('outer nb'); end;
begin
  declare
    procedure a is begin nb; end;
  begin
    a;
  end;
end;
/
create or replace procedure case_c is
  r c%rowtype;
  cursor c is select 1 a from dual;
begin
  say('x');
end;
/
