begin
  select 1 -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from dual;
end;
/

begin
  if (true) then
    select 1 -- Noncompliant
      into var
      from dual;
  end if;
end;
/

begin

    begin
      select 1 -- Compliant, because strict mode is disabled
        into var
        from dual;
    end;

exception
  when others then
    null;
end;
/

create procedure foo is
begin
  select 1 -- Noncompliant
    into var
    from dual;
end;
/

create function foo return number is
begin
  select 1 -- Noncompliant
    into var
    from dual;
end;
/

create package body pack is
    procedure foo is
    begin
      select 1 -- Noncompliant
        into var
        from dual;
    end;

    function foo return number is
    begin
      select 1 -- Noncompliant
        into var
        from dual;
    end;
end;
/

-- correct code
begin
  select 1
    into var
    from dual;
exception
  when others then
    null;
end;
/

begin -- outer block doesn't require an exception handling block
    begin
      select 1
        into var
        from dual;
    exception
      when others then
        null;
    end;
end;
/

create trigger foo
before insert on tab
begin
  select 1
    into var
    from dual;
exception
  when others then
    null;
end;
/

-- A handler covers the statements of its own block only.
begin
  null;
exception
  when others then
    select 1 -- Noncompliant
      into var
      from dual;
end;
/

begin
  begin
    null;
  exception
    when others then
      select 1 -- covered by the enclosing block
        into var
        from dual;
  end;
exception
  when others then
    null;
end;
/

begin
  begin
    select 1 -- covered by the enclosing block
      into var
      from dual;
  end;
exception
  when others then
    null;
end;
/

-- A nested subprogram counts the handler of its enclosing unit.
create procedure outer_with_handler is
  procedure nested is
  begin
    select 1
      into var
      from dual;
  end;
begin
  nested;
exception
  when others then
    null;
end;
/
