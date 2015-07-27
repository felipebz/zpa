-- noncompliant
begin
  select empno
    into var
    from emp;
exception
  when too_many_rows then
    null;
end;

-- compliant
begin
  select empno
    into var
    from emp;
exception
  when too_many_rows then
    var := null;
end;

begin
  select empno
    into var
    from emp;
exception
  when too_many_rows then
    log(sqlerrm);
    raise myexception;
end;