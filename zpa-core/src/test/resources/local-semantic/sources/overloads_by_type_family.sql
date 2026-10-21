create or replace procedure case_a is
  subtype dt_t is date;
  subtype n_t is pls_integer;
  d dt_t := sysdate;
  n n_t := 3;
  procedure f(p number) is begin say('f(number)'); end;
  procedure f(p date) is begin say('f(date)'); end;
  procedure f(p varchar2) is begin say('f(varchar2)'); end;
begin
  f(1); f(1.5); f(sysdate); f('a'); f(d); f(n); f(date '2020-01-01'); f(to_char(1)); f(1 + 1); f(d + 1);
end;
/
create or replace procedure case_b is
  procedure f(p number) is begin say('f(number)'); end;
  procedure f(p date) is begin say('f(date)'); end;
  procedure f(p varchar2) is begin say('f(varchar2)'); end;
begin
  f(null);
end;
/
create or replace procedure case_c is
  procedure f(p number) is begin say('f(number)'); end;
  procedure f(p date) is begin say('f(date)'); end;
  procedure f(p varchar2) is begin say('f(varchar2)'); end;
begin
  f(systimestamp);
end;
/
create or replace procedure case_d is
  procedure f(p number) is begin say('f(number)'); end;
  procedure f(p date) is begin say('f(date)'); end;
begin
  f('2020-01-01');
end;
/
