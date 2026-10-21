create table tb(a number(2), b varchar2(5));
create or replace procedure case_a is
  tb number := 1;
  r tb%rowtype;
begin
  say('r ok');
end;
/
create or replace procedure case_b is
  type rec_t is record(a varchar2(10));
  tb rec_t;
  x tb.a%type := 'abc';
begin
  say('x='||x||' (bound local record field varchar2)');
end;
/
create or replace procedure case_c is
  type rec_t is record(z varchar2(10));
  tb rec_t;
  x tb.a%type;
begin
  say('x ok -> fell back to table column');
end;
/
