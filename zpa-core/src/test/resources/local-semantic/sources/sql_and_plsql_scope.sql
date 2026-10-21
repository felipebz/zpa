create table emp(x number, name varchar2(10));
insert into emp values (1, 'a');
insert into emp values (2, 'b');
insert into emp values (3, 'c');
commit;
create or replace procedure case_a is
  x number := 2;
  x2 number := 2;
  n number;
  m number;
begin
  select count(*) into n from emp where x = x;
  select count(*) into m from emp where x = x2;
  say('where x = x -> '||n||' ; where x = x2 -> '||m);
end;
/
create or replace procedure case_b is
  n number;
begin
  <<blk>>
  declare
    x number := 2;
  begin
    select count(*) into n from emp where emp.x = blk.x;
    say('blk.x -> '||n);
  end;
end;
/
create or replace procedure case_c is
  x number := 2;
  n number;
begin
  select count(*) into n from emp where x = case_c.x;
  say('case_c.x -> '||n);
end;
/
create or replace procedure case_d is
  x number := 99;
  n number;
  name varchar2(10) := 'zz';
begin
  insert into emp(x, name) values (x, name);
  select count(*) into n from emp where x = 99 and name = 'zz';
  say('insert values used locals -> '||n);
  select x into n from emp where rownum = 1 and x = 1;
  say('select-list x -> '||n);
  rollback;
end;
/
create or replace procedure case_e is
  n number;
  function f return number is begin return 1; end;
begin
  select f into n from dual;
end;
/
create or replace procedure case_f is
  n number;
  r emp%rowtype;
begin
  r.x := 3;
  select count(*) into n from emp where x = r.x;
  say('record field r.x -> '||n);
end;
/
