declare
  var number; -- Noncompliant {{Remove this unused "VAR" local variable.}}
  i number; -- Noncompliant {{Remove this unused "I" local variable.}}
  
  procedure proc is
    proc_var number;  -- Noncompliant {{Remove this unused "PROC_VAR" local variable.}}
  begin
    null;
  end;
  
  function func return number is
    func_var number;  -- Noncompliant {{Remove this unused "FUNC_VAR" local variable.}}
  begin
    null;
  end;
begin
  null;
  
  declare
    var2 number; -- Noncompliant {{Remove this unused "VAR2" local variable.}}
  begin
    null;
  end;
  
  for i in 1..2 loop -- this "i" variable is not the same as the one in declaration section
    func(i);
  end loop;
end;
/

create or replace package body test is
  package_body_var number;  -- Noncompliant {{Remove this unused "PACKAGE_BODY_VAR" local variable.}}
  hidden_var number;  -- Noncompliant {{Remove this unused "HIDDEN_VAR" local variable.}}
  "VAR" number; -- Noncompliant {{Remove this unused "VAR" local variable.}}
  "var" number;
  
  procedure proc is
    hidden_var number; -- this declaration hides the previous one
    var number; -- this declaration hides the previous "VAR"
  begin
    hidden_var := 0;
    var := 0;
    "var" := 0;
  end;
end;
/

-- correct
declare
  simple_var number;
  into_var number;
  insert_var number;
  comparision_var number;
  case_insensitive_test number;
  record_variable rectype;
  error exception;
begin
  simple_var := 0;
  CASE_INSENSITIVE_TEST := 0;
  record_variable.field := 0;
  
  select 1
    into into_var
    from dual;
    
  insert into tab (x) values (insert_var);
  
  if (comparision_var = 1) then
    null;
  end if;
  
  for i in 1..2 loop -- "i" variable is not used, but it is required here
    null;
  end loop;
  
  raise error;
end;
/

create or replace package test is
  package_body_var number; -- do not report error on package specifications
end;
/

create or replace package body test is
  package_body_var number;
  qualified_var number;
  
  procedure proc is
  begin
    package_body_var := 0;
    test.qualified_var := 0;
    test.var := 0;
  end;
end;
/

-- A use through the block label.
<<main>>
declare
  i number := 5;
begin
  for i in 1..1 loop
    dbms_output.put_line(main.i);
  end loop;
end;
/

-- A record field named like the variable is not a use of the variable.
declare
  type r_t is record (i number);
  type tab_t is table of r_t index by pls_integer;
  fonts tab_t;
  i pls_integer; -- Noncompliant {{Remove this unused "I" local variable.}}
begin
  fonts(1).i := 1;
end;
/

-- The loop index shadows the variable: the references are the index's.
declare
  i number; -- Noncompliant {{Remove this unused "I" local variable.}}
begin
  for i in 1..2 loop
    dbms_output.put_line(i);
  end loop;
end;
/

-- In SQL a column of the same name may take the name: not provably unused without the table's columns.
declare
  x number;
  n number;
begin
  select count(x) into n from some_table;
  dbms_output.put_line(n);
end;
/

-- A use inside conditional compilation may exist: not provably unused.
declare
  v number;
begin
  $if $$use_v $then
  null;
  $else
  v := 1;
  $end
  null;
end;
/

-- Substitution text may hold a use: not provably unused.
declare
  w number;
  n number;
begin
  n := 1 &plus_w;
  dbms_output.put_line(n);
end;
/

-- Two declarations of one name in a block: neither is used, so both are reported.
declare
  id boolean; -- Noncompliant {{Remove this unused "ID" local variable.}}
  id varchar2(5); -- Noncompliant {{Remove this unused "ID" local variable.}}
begin
  null;
end;
/

-- A reference names both declarations (Oracle rejects it with PLS-00371), so neither is reported.
declare
  dup boolean;
  dup varchar2(5);
begin
  dup := 'a';
end;
/

-- A reference written before its declaration still names it (Oracle rejects it), so neither is reported.
declare
  a number := b;
  b number := 1;
begin
  dbms_output.put_line(a);
end;
/
