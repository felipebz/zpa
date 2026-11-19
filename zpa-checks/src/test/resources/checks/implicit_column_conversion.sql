create table box_tag (tag varchar2(5), weight number);

create or replace procedure run_check(p_num number, p_code varchar2, p_day date) is
  n number;
  v_code varchar2(5);
  v_id crate.id%type;
  v_weight box_tag.weight%type;
begin
  select count(*) into n from crate where code = 10; -- Noncompliant {{Oracle converts column "CODE" to a number to compare it: an index on it is not used, and a value that is not a number raises ORA-01722.}}
  select count(*) into n from crate where code = p_num; -- Noncompliant
  select count(*) into n from crate where 10 < code; -- Noncompliant
  select count(*) into n from crate where code in (10, 20); -- Noncompliant
  select count(*) into n from crate where code not in ('A', 20); -- Noncompliant
  select count(*) into n from crate where code between 10 and 20; -- Noncompliant
  select count(*) into n from crate c join bin b on b.id = c.code; -- Noncompliant
  select count(*) into n from crate where code = date '2026-01-01'; -- Noncompliant {{Oracle converts column "CODE" to a date to compare it: an index on it is not used, and a value that is not a date raises an error.}}
  select count(*) into n from crate where code = p_day; -- Noncompliant
  select count(*) into n from crate where fixed = 10; -- Noncompliant
  select count(*) into n from crate where wide = 10; -- Noncompliant
  select count(*) into n from crate group by code having code > 1; -- Noncompliant
  select count(*) into n from crate start with code = 1 connect by prior id = id; -- Noncompliant
  select count(*) into n from box_tag where tag = 10; -- Noncompliant
  select count(*) into n from crate where code = v_id; -- Noncompliant
  select count(*) into n from box_tag where tag = v_weight; -- Noncompliant

  -- Not reported.
  select count(*) into n from crate where code = '10';
  select count(*) into n from crate where code = p_code;
  select count(*) into n from crate where id = '10';
  select count(*) into n from crate where to_number(code) = 10;
  select count(*) into n from crate where code = to_char(p_num);
  select count(*) into n from crate where trim(code) = 10;
  select count(*) into n from crate where note = 10;
  select count(*) into n from crate where loose = 10;
  select count(*) into n from crate where code = n + 1;
  select count(*) into n from crate where code = any (10, 20);
  select count(*) into n from crate where code in (select id from bin);
  select count(*) into n from box_tag where weight = '10';
  update crate set code = 10 where id = 1;
  insert into crate (code) values (10);
  select id into v_code from crate where id = 1;
  if v_code = 10 then
    null;
  end if;
end;
/
