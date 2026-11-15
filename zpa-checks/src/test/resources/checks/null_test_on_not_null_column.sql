create procedure run_check is
  n number;
  v_code varchar2(10);
  cursor c_orders is select code from orders where code is null; -- Noncompliant
begin
  select count(*) into n from orders where code is null; -- Noncompliant {{Column "CODE" is never null, so this condition is always false.}}
--                                         ^^^^^^^^^^^^
  select count(*) into n from orders o where o.code is not null; -- Noncompliant {{Column "CODE" is never null, so this condition is always true.}}
  select count(*) into n from orders where note is null;
  select count(*) into n from orders o left join items i on i.code = o.code where i.code is null;
  select count(*) into n from orders o, items i where i.code(+) = o.code and i.code is null;
  update orders set note = 'x' where code is not null; -- Noncompliant
  delete from orders where note = 'x' and code is null; -- Noncompliant
  for r in (select code from orders where code is not null) loop null; end loop; -- Noncompliant
  merge into orders o using items i on (o.code = i.code) when matched then update set o.note = 'y' where o.code is not null; -- Noncompliant

  -- Each side an outer join can fill with nulls.
  select count(*) into n from items i right join orders o on i.code = o.code where i.code is null;
  select count(*) into n from orders o full join items i on i.code = o.code where o.code is null;
  select count(*) into n from items i left join (orders o join items j on j.code = o.code) on o.code = i.code where o.code is null;
  select count(*) into n from items i left join orders o join items j on j.code = o.code on o.code = i.code where j.code is null;
  select count(*) into n from orders o, items i where o.code = i.code(+) and o.code is null; -- Noncompliant
  select count(*) into n from orders o right join items i on i.code = o.code where i.code is null; -- Noncompliant
  select count(*) into n from orders o join items i on i.code = o.code where i.code is null; -- Noncompliant

  -- What the dictionary says for each kind of constraint.
  select count(*) into n from nn_tab where code is null; -- Noncompliant
  select count(*) into n from novalidate_tab where code is null;
  select count(*) into n from disabled_tab where code is null;
  select count(*) into n from deferred_tab where code is null;
  select count(*) into n from pk_tab where code is null; -- Noncompliant
  select count(*) into n from check_tab where code is null;
  select count(*) into n from nn_view where code is null; -- Noncompliant
  select count(*) into n from oj_view where code is null;
  select count(*) into n from unstated_tab where code is null;

  -- Not a column the metadata knows to exist.
  select count(*) into n from orders where v_code is null;
  select count(*) into n from (select code from orders) where code is null;
  select count(*) into n from orders where missing_col is null;

  -- Not a condition of a WHERE.
  select count(*) into n from orders o join items i on o.code is not null;
  select case when code is null then 1 end into n from orders where rownum = 1;
end;
/
