create or replace package pk_ok is
  procedure a;
  procedure b;
end;
/
create or replace package body pk_ok is
  procedure a is begin b; end;
  procedure b is begin say('pk_ok.b'); end;
end;
/
create or replace package pk_bad is
  procedure a;
end;
/
create or replace package body pk_bad is
  procedure a is begin c; end;
  procedure c is begin say('c'); end;
end;
/
create or replace package pk_dup is
  x number := 1;
  procedure show;
end;
/
create or replace package body pk_dup is
  x number := 2;
  procedure show is begin say('x='||x); end;
end;
/
create or replace package pk_ord is
  c1 constant number := c2;
  c2 constant number := 1;
end;
/
create or replace package pk_q is
  x varchar2(10) := 'spec x';
  procedure show;
end;
/
create or replace package body pk_q is
  g varchar2(10) := 'private g';
  procedure show is
    x varchar2(10) := 'local x';
  begin
    say('pk_q.g='||pk_q.g||' pk_q.x='||pk_q.x||' x='||x);
  end;
end;
/
create or replace package pk_bord is
  procedure show;
end;
/
create or replace package body pk_bord is
  v1 number := v2;
  v2 number := 3;
  procedure show is begin say('v1='||v1); end;
end;
/
create or replace procedure case_a is
begin
  pk_ok.a; pk_q.show;
end;
/
