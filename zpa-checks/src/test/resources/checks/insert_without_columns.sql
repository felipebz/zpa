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
