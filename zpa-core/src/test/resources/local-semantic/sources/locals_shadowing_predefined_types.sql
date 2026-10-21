create or replace procedure case_a is
  subtype integer is date;
  v integer := date '2001-01-01';
  w standard.integer := 5;
  function f(p integer) return varchar2 is begin return to_char(p,'YYYY'); end;
begin
  say('v='||to_char(v,'YYYY')||' w='||w||' f='||f(date '2002-01-01'));
end;
/
create or replace procedure case_b is
  subtype pls_integer is varchar2(10);
  v pls_integer := 'abc';
begin
  say('v='||v);
end;
/
create or replace procedure case_c is
  subtype number is date;
begin
  say('local subtype number ok');
end;
/
create or replace procedure case_d is
  integer number := 5;
  v integer;
begin
  say('variable named integer then used as type ok');
end;
/
create or replace procedure case_e is
  v1 integer := 1;
begin
  declare
    subtype integer is varchar2(5);
    v2 integer := 'abc';
  begin
    say('inner v2='||v2);
  end;
  declare
    v3 integer := 7;
  begin
    say('after shadow block v3='||v3||' v1='||v1);
  end;
end;
/
create or replace procedure case_f is
  subtype "NUMBER" is date;
  v number := date '2003-01-01';
begin
  say('quoted NUMBER subtype; v year='||to_char(v,'YYYY'));
end;
/
