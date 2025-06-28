declare
  cursor cur is
    select * -- Noncompliant {{SELECT * should not be used.}}
      from emp;

  row emp%rowtype;

  type my_array is table of emp%rowtype;
  row_table my_array;
begin
  -- violations
  select * -- Noncompliant {{SELECT * should not be used.}}
    into var
    from emp;

  select emp.* -- Noncompliant
    into var
    from emp;

  select distinct emp.* -- Noncompliant
    into var
    from emp;

  select user.emp.* -- Noncompliant
    into var
    from user.emp;

  select * -- Noncompliant
    into pkg.var
    from emp;

  select *
    into row_table(1)
    from emp;

  -- valid code
  select emp.empno
    into var
    from emp;

  select emp.sal * 2 -- does not match the multiplication operator
    into var
    from emp;

  select count(*) -- count is acceptable
    into var
    from emp;

  select emp.sal
    into var
    from emp
   where exists (select * from e); -- in a exists expression is acceptable too

  select *
    into row
    from emp;

  select *
    bulk collect into row_table
    from emp;
end;
/
declare
  v_foo emp%rowtype;

  cursor cur is
    select * -- Compliant
      from emp;
begin
  open cur;
  fetch cur into v_foo;
  close cur;
end;
/
