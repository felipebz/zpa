declare
  var number; -- violation
  i number; -- violation
  
  procedure proc is
    proc_var number;  -- violation
  begin
    null;
  end;
  
  function func return number is
    func_var number;  -- violation
  begin
    null;
  end;
begin
  null;
  
  declare
    var2 number; -- violation
  begin
    null;
  end;
  
  for i in 1..2 loop -- this "i" variable is not the same as the one in declaration section
    func(i);
  end loop;
end;
/

create or replace package body test is
  package_body_var number;  -- violation
  hidden_var number;  -- violation
  
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