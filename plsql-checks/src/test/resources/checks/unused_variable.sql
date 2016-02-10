declare
  var number; -- Noncompliant {{Remove this unused "var" local variable.}}
  i number; -- Noncompliant {{Remove this unused "i" local variable.}}
  
  procedure proc is
    proc_var number;  -- Noncompliant {{Remove this unused "proc_var" local variable.}}
  begin
    null;
  end;
  
  function func return number is
    func_var number;  -- Noncompliant {{Remove this unused "func_var" local variable.}}
  begin
    null;
  end;
begin
  null;
  
  declare
    var2 number; -- Noncompliant {{Remove this unused "var2" local variable.}}
  begin
    null;
  end;
  
  for i in 1..2 loop -- this "i" variable is not the same as the one in declaration section
    func(i);
  end loop;
end;
/

create or replace package body test is
  package_body_var number;  -- Noncompliant {{Remove this unused "package_body_var" local variable.}}
  hidden_var number;  -- Noncompliant {{Remove this unused "hidden_var" local variable.}}
  
  procedure proc is
    hidden_var number; -- this declaration hides the previous one
  begin
    hidden_var := 0;
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
  
  procedure proc is
  begin
    package_body_var := 0;
    test.var := 0;
  end;
end;
/