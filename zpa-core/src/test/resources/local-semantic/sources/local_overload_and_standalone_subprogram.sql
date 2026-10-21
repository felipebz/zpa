create or replace procedure f_local(p number) is begin say('standalone f_local(number)'); end;
/
create or replace procedure case_a is
  procedure f_local(p varchar2) is begin say('local f_local(varchar2)'); end;
begin
  f_local(1);
end;
/
create or replace procedure case_b is
  procedure f_local(p date) is begin say('local f_local(date)'); end;
begin
  f_local(1);
end;
/
