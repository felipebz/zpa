create or replace package testpkg is

  -- %suite

  -- Noncompliant@+2 {{Fix or remove this disabled unit test.}}
  -- %test
  -- %disabled
  procedure test;

  -- %test
  -- %disabled with some reason
  procedure test2;

end testpkg;
