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
end;