create procedure run_check is
  n number;
begin
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where c.color = 'RED'; -- Noncompliant {{This condition discards the rows the outer join adds for "c"; move it to the ON clause or allow null.}}
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id and c.color = 'RED';
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where c.id is null;
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where c.color = 'RED' or c.id is null;
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where nvl(c.color, 'RED') = 'RED';
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where c.color || 'x' = 'x';
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where c.id is not null; -- Noncompliant
  select count(*) into n from shelf s, crate c where c.shelf_id(+) = s.id and c.color = 'RED'; -- Noncompliant
  select count(*) into n from shelf s, crate c where c.shelf_id(+) = s.id and c.color(+) = 'RED';
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id join label l on l.id = c.label_id; -- Noncompliant
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id left join label l on l.id = c.label_id;
  select count(*) into n from shelf s left join (crate c join label l on l.id = c.label_id) on c.shelf_id = s.id;
  select count(*) into n from shelf s full join crate c on c.shelf_id = s.id where s.code = 'A'; -- Noncompliant {{This condition discards the rows the outer join adds for "s"; move it to the ON clause or allow null.}}
  select count(*) into n from shelf s right join crate c on c.shelf_id = s.id where s.code = 'A'; -- Noncompliant
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where s.code = 'A';
  -- Every operand of an AND chain, LIKE, BETWEEN and IN with a list reject null, NOT LIKE as well.
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where s.code = 'A' and (c.color like 'R%'); -- Noncompliant
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where c.id between 1 and 9; -- Noncompliant
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where c.id in (1, 2); -- Noncompliant
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where c.color not like 'R%'; -- Noncompliant
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where 'RED' = c.color; -- Noncompliant
  -- Under NOT, a function, CASE, a subquery or ALL: not reported.
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where not (c.color = 'RED');
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where upper(c.color) = 'RED';
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where case when c.id > 1 then 1 end = 1;
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where c.id in (select id from label);
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where c.id <> all (1, 2);
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where exists (select 1 from label x where x.id = c.label_id);
  -- An inner join before the outer join, or a view whose own query has the outer join.
  select count(*) into n from shelf s join label l on l.id = s.id left join crate c on c.shelf_id = s.id;
  select count(*) into n from (select c.color from shelf s left join crate c on c.shelf_id = s.id) v where v.color = 'RED';
  select count(*) into n from shelf s left join (select id, color, shelf_id from crate) v on v.shelf_id = s.id where v.color = 'RED'; -- Noncompliant
  -- A repeated alias: Oracle reads the nearest source, the inner join's own.
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id join crate c on c.shelf_id = s.id;
  -- Under NOT BETWEEN only the tested operand rejects null; under BETWEEN every operand does.
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where s.n not between c.lo and 10;
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where s.n not between 1 and c.hi;
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where c.lo not between 1 and 10; -- Noncompliant
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where s.n between c.lo and 10; -- Noncompliant
end;
/
