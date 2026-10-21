create or replace procedure case_a is
  v t;
  subtype t is number;
begin
  say('v='||v);
end;
/
create or replace procedure case_b is
  subtype t is varchar2(10);
begin
  declare
    v t;
    subtype t is number;
  begin
    v := 'abc';
    say('v='||v);
  end;
end;
/
create or replace procedure case_c is
  subtype t is varchar2(10);
begin
  declare
    v t;
    subtype t is number;
  begin
    say('unreferenced v; inner t is later decl');
  end;
end;
/
