begin
  select empno
    into var
    from emp;
exception
  when too_many_rows then -- Noncompliant {{Fill this TOO_MANY_ROWS exception handler.}}
    null;
end;

begin
  select empno
    into var
    from emp;
exception
  when no_data_found or too_many_rows then -- Noncompliant
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

begin
  select empno
    into var
    from emp;
exception
  when no_data_found then
    null;
  when pkg.error then
    var := null;
  when others then
    null;
end;