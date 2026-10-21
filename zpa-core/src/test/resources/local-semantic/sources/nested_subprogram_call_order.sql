create or replace procedure later_proc is begin say('standalone later_proc'); end;
/
create or replace procedure case_a is
  procedure a is begin b_local; end;
  procedure b_local is begin say('local b'); end;
begin
  a;
end;
/
create or replace procedure case_b is
  procedure a is begin later_proc; end;
  procedure later_proc is begin say('local later_proc'); end;
begin
  a;
  later_proc;
end;
/
create or replace procedure case_c is
  procedure later_proc;
  procedure a is begin later_proc; end;
  procedure later_proc is begin say('local later_proc'); end;
begin
  a;
end;
/
