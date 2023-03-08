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
