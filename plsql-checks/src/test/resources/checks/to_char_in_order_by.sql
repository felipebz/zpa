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
