declare
  var tab%rowtype;

  subtype my_type is tab%rowtype;
  v_my_type my_type;

  type t_tab is table of tab%rowtype index by binary_integer;
  v_tab t_tab;
begin
  insert into tab values (1); -- Noncompliant {{Specify the columns in this INSERT.}}
  
  insert into tab (col) values (1);

  insert into tab values var;

  insert into tab values var returning id into v_id;

  for r_t in (select * from mytable) loop
      insert into mytable values r_t;
  end loop;

  forall idx in v_tab.first .. v_tab.last
  insert into tab values v_tab(idx);

  insert into tab values v_tab(idx);

  insert into tab values v_my_type;

  insert into tab values (1), (2); -- Noncompliant {{Specify the columns in this INSERT.}}

  insert into tab set col = 1;

  insert into tab set (col = 1), (col = 2);

  insert into tab by name select 1 col from dual;

  insert into tab by position select 1 from dual; -- Noncompliant {{Specify the columns in this INSERT.}}

end;
create or replace package sample_pkg is
  type row_table is table of tab%rowtype index by binary_integer;

  procedure insert_rows (rows in row_table);
  procedure insert_rows_qualified (rows in sample_pkg.row_table);
end sample_pkg;
/
create or replace package body sample_pkg is
  procedure insert_rows (rows in row_table) is
  begin
    insert into tab values rows(1);
  end;

  procedure insert_rows_qualified (rows in sample_pkg.row_table) is
  begin
    insert into tab values rows(1);
  end;
end sample_pkg;
/

create or replace procedure record_inserts is
  type rec is record (a number, b varchar2(10));
  r_rec rec;
  type outer_rec is record (r tab%rowtype, k number);
  x outer_rec;
  cursor c is select a, b from tab;
  r_cur c%rowtype;
  cursor c_all is select * from tab;
  r_all c_all%rowtype;
  cursor c_alias is select t.* from tab t where t.a > 0;
  cursor c_other is select * from other_tab;
  r_other_cur c_other%rowtype;
  r_other other_tab%rowtype;
  r_owned app.tab%rowtype;
  type cur_rows is table of c%rowtype index by pls_integer;
  v_cur_rows cur_rows;
  r_row tab%rowtype;
  type numbers is table of number;
  v_numbers numbers;
  type recs is table of rec index by pls_integer;
  v_recs recs;
begin
  -- a record maps to the columns by position, as a value list does
  insert into tab values r_rec; -- Noncompliant
  insert into tab values v_recs(1); -- Noncompliant

  -- the row of a query with its own select list never follows the target's columns: a new column in tab breaks it
  insert into tab values r_cur; -- Noncompliant
  insert into tab values v_cur_rows(1); -- Noncompliant
  for r_loop in c loop
    insert into tab values r_loop; -- Noncompliant
  end loop;
  for r_loop in (select b, a from tab) loop
    insert into tab values r_loop; -- Noncompliant
  end loop;

  -- the target's own %ROWTYPE, or a query of all its columns, follows its columns
  insert into tab values r_row;
  insert into tab values r_all;
  for r_loop in c_alias loop
    insert into tab values r_loop;
  end loop;

  -- another name may be a synonym or a view of tab, and a schema prefix on one side only may name tab: undecided
  insert into tab values r_other;
  insert into tab values r_other_cur;
  insert into tab values r_owned;

  -- a field of tab%rowtype type is skipped as before
  insert into tab values x.r;

  -- with a column list Oracle rejects the record (ORA-00947), but that is not this rule
  insert into tab (a, b) values r_row;

  -- parentheses make it a list of column values
  insert into tab values (r_row); -- Noncompliant

  -- an element of a collection of scalars is not a row
  insert into tab values v_numbers(1); -- Noncompliant
end;
/
-- A loop over a cursor with a RETURN type has that type's row; c%rowtype is still the row of the query.
create or replace procedure cursor_return_type is
  cursor c return tab%rowtype is select a, b from tab;
  r c%rowtype;
begin
  insert into tab values r; -- Noncompliant
  for r2 in c loop
    insert into tab values r2;
  end loop;
end;
/
-- Conditional compilation may declare another "r": nothing is proven.
create or replace procedure conditional_record is
  r tab%rowtype;
  $if $$plain $then
  r number;
  $end
begin
  insert into tab values r;
end;
/
-- The specification of pkg is not in this file: nothing proves what the names are.
create package body pkg is
  procedure p is
  begin
    insert into tab values r_row;
    insert into tab values n;
  end;
end;
/
