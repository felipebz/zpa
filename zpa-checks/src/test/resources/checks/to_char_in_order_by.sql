select *
from emp
order by to_char(empno); -- Noncompliant
--       ^^^^^^^^^^^^^^

select to_char(empno)
from emp
order by 1; -- Noncompliant
--       ^

select *
from emp
order by empno; -- Compliant

select *
from emp
order by 'employee'; -- Compliant

select *
from emp
order by null; -- Compliant

select empno
from emp
order by 1; -- Compliant

select empno, row_number() over (order by 0) -- Compliant
from emp
order by empno;

/* Actually this code causes the error "ORA-01785: ORDER BY item must be the number of a SELECT-list expression".
 * It's still compliant as this is not the purpose of this rule.
 */
select empno
from emp
order by 0; -- Compliant

declare
  my_empno number;
begin
  select *
  into foo
  from emp
  order by my_empno; -- Compliant
end;

-- Oracle reads any numeric literal as a position, truncated, also behind a unary plus or parentheses.
select to_char(empno), ename from emp order by 1.0; -- Noncompliant
select to_char(empno), ename from emp order by 1.5; -- Noncompliant
select to_char(empno), ename from emp order by 1e0; -- Noncompliant
select to_char(empno), ename from emp order by +1; -- Noncompliant
select to_char(empno), ename from emp order by (1); -- Noncompliant
select to_char(empno), ename from emp order by +((1)); -- Noncompliant
select ename, to_char(empno) from emp order by 2.9; -- Noncompliant
select to_char(empno), ename from emp order by 2.5; -- Compliant: position 2 is ename

-- Not a position: a binary float or double literal, or a negative number, is a constant.
select to_char(empno), ename from emp order by 1f; -- Compliant
select to_char(empno), ename from emp order by 1d; -- Compliant
select to_char(empno), ename from emp order by -1; -- Compliant

-- Out of range (ORA-01785): not this rule.
select to_char(empno), ename from emp order by 99999999999; -- Compliant
select to_char(empno), ename from emp order by 0.5; -- Compliant
select to_char(empno), ename from emp order by 1e99999999; -- Compliant
select to_char(empno), ename from emp order by 1e999999999; -- Compliant
select to_char(empno), ename from emp order by 1e-999999999; -- Compliant
