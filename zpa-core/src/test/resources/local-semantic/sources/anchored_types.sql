create table tbl(a number(2) not null, b varchar2(5));
create or replace procedure case_a is
  v number(2) := 1;
  w v%type;
begin
  begin
    w := 100;
    say('w=100 ok (no size inherited)');
  exception when value_error then say('value_error: size inherited via %TYPE');
  end;
end;
/
create or replace procedure case_b is
  v number not null := 1;
  w v%type;
begin
  say('w uninitialised ok');
end;
/
create or replace procedure case_c is
  type r_t is record(a number, b varchar2(3));
  rec r_t;
  f rec.b%type;
begin
  begin
    f := 'abcd';
    say('f=abcd ok');
  exception when value_error then say('value_error: rec.b%TYPE is varchar2(3)');
  end;
end;
/
create or replace procedure case_d is
  type r_t is record(a number, b varchar2(3));
  f r_t.b%type;
begin
  say('type-name field anchor ok');
end;
/
create or replace procedure case_e is
  r tbl%rowtype;
  x tbl.a%type;
  cursor c is select a, b, 'z' extra from tbl;
  cr c%rowtype;
begin
  r.a := 1; x := 2; cr.extra := 'z';
  say('rowtype ok r.a='||r.a||' x='||x||' cr.extra='||cr.extra);
  begin
    x := 100;
    say('x=100 ok');
  exception when value_error then say('value_error: column precision inherited');
  end;
end;
/
create or replace procedure case_f is
  x tbl.a%type;
begin
  say('column NOT NULL not inherited (no init needed)');
end;
/
