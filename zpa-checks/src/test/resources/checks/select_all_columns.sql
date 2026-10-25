create or replace package pkg is
  var number;
end;
/
declare
  cursor cur is
    select * -- Noncompliant {{SELECT * should not be used.}}
      from emp;

  row emp%rowtype;

  type my_array is table of emp%rowtype;
  row_table my_array;

  var number;
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

  -- an undeclared destination may be a row
  select *
    into undeclared_var
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

declare
  type rec is record (empno number, ename varchar2(10));
  r_rec rec;
  cursor c is select * from emp; -- Noncompliant
  r_cur c%rowtype;
  type recs is table of rec;
  v_recs recs;
  type numbers is table of number;
  v_numbers numbers;
  type rows is table of emp%rowtype index by pls_integer;
  v_rows rows;
begin
  -- a cursor %ROWTYPE record follows the cursor's shape
  select * into r_cur from emp;

  -- a record maps to the columns by position
  select * into r_rec from emp; -- Noncompliant

  -- collections of a RECORD type, or of a scalar
  select * bulk collect into v_recs from emp; -- Noncompliant
  select * bulk collect into v_numbers from emp; -- Noncompliant

  -- a collection of emp%rowtype, whole or one element
  select * bulk collect into v_rows from emp;
  select * into v_rows(1) from emp;

  -- the cursor is only fetched into a RECORD type variable
  open c;
  fetch c into r_rec;
  close c;
end;
/
