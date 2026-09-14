begin 
  select 1 -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from dual;
end;
/

begin
  -- This is not valid code as it causes "PLS-00428: an INTO clause is expected in this SELECT statement" when compiling on
  -- the database, but I'll keep this example here because the file is being parser correctly and the check shouldn't crash.
  select 1 -- Noncompliant {{Handle exceptions of this query.}}
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
      select 1 -- Noncompliant, inner block without exception handling
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

-- queries with bulk collect should be ignored because they don't throw no_data_found/too_many_rows
begin
  select 1
    bulk collect into var
    from dual;
end;
/

-- A non-BULK single-group aggregate query has exactly one result row unless
-- an outer construct changes that cardinality.
begin
  select count(*)
    into var
    from dual
   where 1 = 0;
end;
/

begin
  select sum(value)
    into var
    from values_table;
end;
/

begin
  select max(value)
    into var
    from values_table;
end;
/

begin
  select avg(value)
    into var
    from values_table;
end;
/

begin
  select min(value)
    into var
    from values_table;
end;
/

begin
  select case when count(*) > 0 then 1 else 0 end
    into var
    from values_table;
end;
/

begin
  select distinct count(*)
    into var
    from values_table;
end;
/

begin
  select listagg(value, ',') within group (order by value)
    into var
    from values_table;
end;
/

begin
  select count(*) -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from values_table
   group by value;
end;
/

begin
  select count(*) -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from values_table
   having count(*) > 0;
end;
/

begin
  select count(*) over () -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from values_table;
end;
/

begin
  select count(*) -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from values_table
  union all
  select count(*)
    from other_values;
end;
/

begin
  select count(*) -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from values_table
  union
  select count(*)
    from other_values;
end;
/

begin
  select count(*) -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from values_table
   order by value
   offset 1 rows;
end;
/

begin
  select count(*) -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from values_table
  fetch first 1 row only;
end;
/

begin
  select (select count(*) from values_table) -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from other_values;
end;
/

begin
  select value -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from (select count(*) as value from values_table) aggregated_values;
end;
/

begin
  select value -- Noncompliant {{Handle exceptions of this query.}}
    into var
    from values_table;
end;
/
