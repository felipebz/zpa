create or replace procedure case_a is
  procedure f(a number) is begin say('f(a)'); end;
  procedure f(b number) is begin say('f(b)'); end;
begin
  f(a => 1); f(b => 2);
end;
/
create or replace procedure case_b is
  procedure f(a number) is begin say('f(a)'); end;
  procedure f(b number) is begin say('f(b)'); end;
begin
  f(1);
end;
/
create or replace procedure case_c is
  procedure f(a number) is begin say('f(a)'); end;
  procedure f(a number, b number) is begin say('f(a,b)'); end;
begin
  f(1); f(1, 2); f(a => 1, b => 2);
end;
/
create or replace procedure case_d is
  procedure f(a number) is begin say('f(a)'); end;
  procedure f(a number, b number default 0) is begin say('f(a,b default)'); end;
begin
  f(1, 2);
end;
/
create or replace procedure case_e is
  procedure f(a number) is begin say('f(a)'); end;
  procedure f(a number, b number default 0) is begin say('f(a,b default)'); end;
begin
  f(1);
end;
/
