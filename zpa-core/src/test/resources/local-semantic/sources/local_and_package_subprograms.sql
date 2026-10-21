create or replace procedure sp is begin say('standalone sp'); end;
/
create or replace package pk_s is
  procedure sp;
  procedure caller;
  procedure caller2;
end;
/
create or replace package body pk_s is
  procedure sp is begin say('package pk_s.sp'); end;
  procedure caller is
    procedure sp is begin say('nested in pk_s.caller'); end;
  begin
    sp; pk_s.sp;
  end;
  procedure caller2 is
  begin
    sp;
  end;
end;
/
create or replace procedure case_a is
  procedure sp is begin say('local sp'); end;
begin
  sp; pk_s.sp; pk_s.caller; pk_s.caller2;
end;
/
begin
  execute immediate 'create or replace procedure case_b is procedure sp is begin say(''local sp''); end; begin sp; ' || user || '.sp; end;';
exception when others then say('create case_b: '||sqlerrm);
end;
/
create or replace procedure case_c is
  sp number := 1;
begin
  sp;
end;
/
