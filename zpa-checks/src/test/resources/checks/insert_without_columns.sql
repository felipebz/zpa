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
  r_row tab%rowtype;
  type numbers is table of number;
  v_numbers numbers;
  type recs is table of rec index by pls_integer;
  v_recs recs;
begin
  -- a record maps to the columns by position, as a value list does
  insert into tab values r_rec; -- Noncompliant
  insert into tab values v_recs(1); -- Noncompliant

  -- a %ROWTYPE record of a cursor, or a field of tab%rowtype type, also maps by position, but is skipped as before
  insert into tab values x.r;
  insert into tab values r_cur;

  -- with a column list Oracle rejects the record (ORA-00947), but that is not this rule
  insert into tab (a, b) values r_row;

  -- parentheses make it a list of column values
  insert into tab values (r_row); -- Noncompliant

  -- an element of a collection of scalars is not a row
  insert into tab values v_numbers(1); -- Noncompliant
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
