create or replace procedure run_check is
  n number;
  v number;
begin
  -- A subquery whose item may be null.
  select count(*) into n from shelf where id not in (select shelf_id from crate); -- Noncompliant {{Column "SHELF_ID" may be null: once a null is selected, this NOT IN is never true.}}
  select count(*) into n from shelf where id not in (select c.shelf_id from crate c where c.color = 'red'); -- Noncompliant
  select count(*) into n from shelf where id <> all (select shelf_id from crate); -- Noncompliant
  select count(*) into n from shelf where id != all (select shelf_id from crate); -- Noncompliant
  select count(*) into n from shelf where id ^= all (select shelf_id from crate); -- Noncompliant
  -- An outer join gives nulls for a column declared NOT NULL.
  select count(*) into n from shelf where id not in (select c.shelf_id from label l left join sealed_crate c on c.label_id = l.id); -- Noncompliant

  -- A list that holds a null.
  select count(*) into n from shelf where code not in ('A', null); -- Noncompliant {{This list holds a null, so this NOT IN is never true.}}
  select count(*) into n from shelf where code not in ('A', ''); -- Noncompliant
  if 2 not in (1, null) then -- Noncompliant
    null;
  end if;
  if n not in (1, '') then -- Noncompliant
    null;
  end if;

  -- A condition of the subquery rejects null.
  select count(*) into n from shelf where id not in (select shelf_id from crate where shelf_id is not null);
  select count(*) into n from shelf where id not in (select shelf_id from crate where shelf_id > 0);
  select count(*) into n from shelf where id not in (select shelf_id from crate where color = 'red' and shelf_id like '1%');
  select count(*) into n from shelf where id not in (select shelf_id from crate where shelf_id between 1 and 9);
  select count(*) into n from shelf where id not in (select shelf_id from crate where shelf_id in (1, 2));
  -- NOT BETWEEN rejects null only for its first operand.
  select count(*) into n from shelf where id not in (select shelf_id from crate where 5 not between shelf_id and 9); -- Noncompliant
  -- An OR does not reject null.
  select count(*) into n from shelf where id not in (select shelf_id from crate where shelf_id > 0 or color = 'red'); -- Noncompliant

  -- The column is never null, or is not known to be nullable.
  select count(*) into n from shelf where id not in (select shelf_id from sealed_crate);
  select count(*) into n from shelf where id not in (select shelf_id from loose_crate);

  -- Not reported.
  select count(*) into n from shelf s where not exists (select 1 from crate c where c.shelf_id = s.id);
  select count(*) into n from shelf where not (id not in (select shelf_id from crate));
  select count(*) into n from shelf where not (code not in ('A', null));
  select count(*) into n from shelf where (id, code) not in (select shelf_id, color from crate);
  select count(*) into n from shelf where id not in (select max(shelf_id) from crate);
  select count(*) into n from shelf where id not in (select shelf_id + 0 from crate);
  select count(*) into n from shelf where id not in (select shelf_id from crate union select shelf_id from crate);
  select count(*) into n from shelf where id not in (select x.shelf_id from (select shelf_id from crate) x);
  select count(*) into n from shelf where id not in (select shelf_id, color from crate);
  select count(*) into n from shelf where id not in (select unknown_col from crate);
  select count(*) into n from shelf where code not in ('A', v);
  select count(*) into n from shelf where code in ('A', null);
  select count(*) into n from shelf where id in (select shelf_id from crate);
  select count(*) into n from shelf where id = all (select shelf_id from crate);
end;
/
