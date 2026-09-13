create or replace package test_pkg as

  -- %suite
  -- %test(Does something)
  -- %beforeeach
  procedure does_something;

end test_pkg;

create or replace package body test_pkg as

  -- Noncompliant@+1 {{This utPLSQL annotation is ineffective in a package body.}}
  -- %test
  -- Noncompliant@+1 {{This utPLSQL annotation is ineffective in a package body.}}
  -- %tags(foo, bar)
  procedure does_something is
  begin
    null;
  end;

  -- Noncompliant@+1 {{This utPLSQL annotation is ineffective in a package body.}}
  --   %ROLLBACK(foo, bar)
  procedure helper is
  begin
    null;
  end;

  -- %future_annotation
  -- this is an ordinary comment containing %test
  procedure another_helper is
  begin
    null; --%test
  end;

end test_pkg;
