declare
  foo number;
begin

  select count(foo) -- Noncompliant {{Looks like there is a "foo" variable in this context. Review if this COUNT is correct.}}
--       ^^^^^^^^^^
    from dual;
    
  select count(all foo) -- Noncompliant {{Looks like there is a "foo" variable in this context. Review if this COUNT is correct.}}
--       ^^^^^^^^^^^^^^
    from dual;

  select count(all foo),   -- Noncompliant
         count(foo)        -- Noncompliant
    from dual;

  -- a column "foo" of "tab" would win over the variable: not proven
  select count(foo)
    from tab;

  select count(all bar), count(*), count(all *), sum(all foo), max(all foo), avg(all foo)
    from tab;

  select (count(all foo)) -- not directly a select column, as for the plain form
    from tab;

  -- don't report an error here, we don't have enough information to know if "bar" is a variable or a column of table "tab"
  select count(bar)
    from tab;

  select count(foo, bar), -- the COUNT function has only 1 parameter, so this isn't the Oracle built-in
         my_count(foo),
         pack.count(foo)
    from tab;
    
  bar := count(foo); -- the Oracle built-in can't be used here
  bar := count(all foo);
end;

select count(foo) from dual; -- we don't have a scope here
select count(all foo) from dual;

declare
  dummy varchar2(1);
begin
  -- DUMMY is the column of DUAL, which wins over the variable
  select count(dummy) into dummy from dual;
end;
/

declare
  foo number;
  $if $$extra $then
  bar number;
  $end
begin
  -- conditional compilation in the block: nothing is proven
  select count(foo) into foo from dual;
end;
/
