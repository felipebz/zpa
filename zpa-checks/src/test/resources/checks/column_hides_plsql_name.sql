create procedure drop_crates(shelf_id number) is
begin
  delete from crate where crate.shelf_id = shelf_id; -- Noncompliant {{Oracle reads the column "SHELF_ID" of "crate" here, not the parameter "shelf_id"; qualify the parameter or rename it.}}
  delete from crate where crate.shelf_id = drop_crates.shelf_id;
end;
/
declare
  n number;
begin
  <<blk>>
  declare
    shelf_id number := 2;
  begin
    select count(*) into n from crate where shelf_id = shelf_id; -- Noncompliant {{Oracle reads the column "SHELF_ID" of "crate" here, not the variable "shelf_id"; qualify the variable or rename it.}}
    select count(*) into n from crate where shelf_id = blk.shelf_id;
  end;
end;
/
create procedure paint(color varchar2) is
begin
  update crate set color = color where id = 1; -- Noncompliant
  update crate set color = paint.color where id = 1;
  insert into crate (id, color) values (9, color);
end;
/
declare
  cursor c(color varchar2) is select id from crate where color = color; -- Noncompliant
begin
  null;
end;
/
create package pk_paint as
  procedure count_color;
end;
/
create package body pk_paint as
  color varchar2(10) := 'GREEN';
  procedure count_color is
    n number;
  begin
    select count(*) into n from crate where crate.color = color; -- Noncompliant
  end;
end;
/
create procedure count_labels(shelf_id number) is
  n number;
begin
  -- LABEL has no SHELF_ID: the column of CRATE takes the name, but it is compared with another column, which may be meant.
  select count(*) into n from crate where exists (select 1 from label l where l.id = shelf_id);
  -- A column compared with anything but itself may be meant as the column.
  select count(*) into n from crate where shelf_id = 5;
  -- The select list, ORDER BY, GROUP BY and INTO read the column on purpose.
  select shelf_id into n from crate where id = 1 group by shelf_id order by shelf_id;
  -- A name no source has, and a source whose columns are not known.
  select count(*) into n from label where id = shelf_id;
  select count(*) into n from zz_crate_unknown where id = shelf_id;
  execute immediate 'delete from crate where shelf_id = shelf_id';
end;
/
create procedure merge_stock(stock number) is
begin
  merge into pallet p using crate c on (p.id = c.id and p.stock = stock) -- Noncompliant
  when matched then update set p.stock = stock; -- Noncompliant
end;
/
