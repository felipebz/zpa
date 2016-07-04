select distinct emp.name
  from emp
 order by emp.id; -- Noncompliant {{This value does not exists in the SELECT clause. Fix this expression or add this value in the SELECT.}}
--        ^^^^^^
 
select distinct substr(emp.name, 0, 10) emp_name
  from emp
 order by emp.name; -- Noncompliant
--        ^^^^^^^^

select distinct emp.name emp_name
  from emp
 order by name; -- Noncompliant FALSE POSITIVE

select distinct name
  from emp
 order by emp.name; -- Noncompliant FALSE POSITIVE

select distinct emp.name
  from emp
 order by length(emp.name); -- Noncompliant FALSE POSITIVE

-- Compliant queries
select distinct substr(emp.name, 0, 10) emp_name
  from emp
 order by substr(emp.name, 0, 10);

select distinct substr(emp.name, 0, 10) emp_name
  from emp
 order by emp_name;

select distinct substr(emp.name, 0, 10) emp_name
  from emp
 order by 1;
 
select distinct emp.name
  from emp
 order by emp.name;
 
 select distinct name
  from emp
 order by name;
 
select distinct emp.name
  from emp
 order by name;
 
select emp.name
  from emp
 order by emp.id;
 
select distinct emp.name
  from emp;
