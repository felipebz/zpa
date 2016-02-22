declare
  ex exception;
  ex_handled exception;
begin
  raise ex; -- Noncompliant {{There is not a exception handler for "ex" here. This will cause a "user-defined exception" error.}}
  
  raise ex_handled; -- ok
  raise ex_unknown; -- ok, we don't have any information about the excepton
  raise pkg.ex; -- ok
exception
  when ex_handled then
    null;
end;
/

declare
  ex exception;
begin
  raise ex; -- Noncompliant, there is an "others" handler for this but we will report a violation becase there is a reference to sqlerrm in others   
exception
  when others then
    log(sqlerrm);
end;
/

declare
  ex exception;
begin
  begin
    raise ex; -- Noncompliant   
  exception
    when others then
      log(sqlerrm);
  end;
end;
/

declare
  ex exception;
begin
  begin
    raise ex;   
  exception
    when ex then
      null;
  end;
  
  if (x) then
    raise ex; -- Noncompliant
  end if;
end;
/

create package body pack is
  ex exception;
  
  procedure test is
  begin
    raise ex; -- we won't report a violation here because we don't have enough information to know if another method in this package handles this exception
  end;
end;
/

declare
  ex exception;
begin
  raise ex;   
exception
  when others then
    null;
end;
/

declare
  ex exception;
begin
  begin
    raise ex;
  end;   
exception
  when ex then
    null;
  when others then
    log(sqlerrm);
end;
/

