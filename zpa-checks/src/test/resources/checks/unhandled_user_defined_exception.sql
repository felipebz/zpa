declare
  ex exception;
  ex_handled exception;
begin
  raise ex; -- Noncompliant {{There is no exception handler for "EX" here. This will cause an "user-defined exception" error.}}
  
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

-- A handler for a same-name exception of another block does not catch it.
declare
  procedure shadowed is
    e exception;
  begin
    declare
      e exception;
    begin
      raise shadowed.e; -- Noncompliant {{There is no exception handler for "SHADOWED.E" here. This will cause an "user-defined exception" error.}}
    exception
      when e then
        null;
    end;
  end;
begin
  shadowed;
end;
/

-- A raise in a block's handlers is not caught by that block's handlers.
declare
  e exception;
begin
  begin
    null;
  exception
    when others then
      raise e; -- Noncompliant
  end;
end;
/

-- It propagates to the enclosing block, which catches it.
declare
  e exception;
begin
  begin
    null;
  exception
    when others then
      raise e;
  end;
exception
  when e then
    null;
end;
/

-- Raised by a function called in a declaration: the block's own handler would not catch it, but callers are not
-- followed, and a caller can name the exception: not reported.
declare
  e exception;
  function f return number is
  begin
    raise e;
  end;
begin
  declare
    x number := f;
  begin
    null;
  exception
    when others then
      null;
  end;
end;
/

-- Declared in the subprogram: only its own blocks can name it; a caller could catch it with OTHERS only.
declare
  procedure p is
    e exception;
  begin
    raise e; -- Noncompliant
  end;
begin
  p;
end;
/

-- Declared in an outer block, raised in an inner one, handled by the outer block.
declare
  e exception;
begin
  begin
    raise e;
  end;
exception
  when e then
    null;
end;
/

-- A local exception that shadows a package one: the handler for the package's does not catch it.
create package shadow_pkg is
  procedure p;
end;
/

create package body shadow_pkg is
  e exception;

  procedure p is
    e exception;
  begin
    raise e; -- Noncompliant
  exception
    when shadow_pkg.e then
      null;
  end;
end;
/

-- Conditional compilation in a block in between: a handler may be hidden there, so nothing is reported.
declare
  e exception;
begin
  begin
    $if $$x $then null; $end
    raise e;
  end;
end;
/

-- Exceptions tied to an error code are raised as that error, and handlers match the code.
declare
  e1 exception;
  pragma exception_init(e1, -20001);
  e2 exception;
  pragma exception_init(e2, -20001);
begin
  raise e1;
exception
  when e2 then
    null;
end;
/

declare
  e exception;
  pragma exception_init(e, 100);
begin
  raise e;
exception
  when no_data_found then
    null;
end;
/

-- Nothing handles it, but the caller gets the error code, not a user-defined exception.
create procedure raise_coded is
  e exception;
  pragma exception_init(e, -20002);
begin
  raise e;
end;
/

-- A pragma the analysis cannot fully see: nothing is reported.
create procedure raise_maybe_coded is
  e exception;
  $if $$coded $then
  pragma exception_init(e, -20003);
  $end
begin
  raise e;
end;
/
