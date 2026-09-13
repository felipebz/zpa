CREATE PACKAGE test_pkg AS
  --%suite

  -- Noncompliant@+2 {{Fix or remove this disabled unit test.}}
  --%test
  --%disabled
  PROCEDURE bare_disabled;

  -- Noncompliant@+2 {{Fix or remove this disabled unit test.}}
  --%test
  --%disabled(temporarily broken)
  PROCEDURE disabled_with_reason;

  -- Noncompliant@+2 {{Fix or remove this disabled unit test.}}
  --%test
  -- %DiSaBlEd(temporarily broken)
  PROCEDURE mixed_case_disabled;

  -- Noncompliant@+2 {{Fix or remove this disabled unit test.}}
  --%test
  --%disabled()
  PROCEDURE empty_reason_disabled;

  -- Noncompliant@+2 {{Fix or remove this disabled unit test.}}
  --%test
  --%disabled with some reason
  PROCEDURE malformed_reason_disabled;

  -- Noncompliant@+2 {{Fix or remove this disabled unit test.}}
  --%test
  --%disabled(unclosed
  PROCEDURE unclosed_reason_disabled;

  --%context(Context)

  -- Noncompliant@+2 {{Fix or remove this disabled unit test.}}
  --%test
  --%disabled(context disabled)
  PROCEDURE context_disabled;

  --%context(Nested context)

  -- Noncompliant@+2 {{Fix or remove this disabled unit test.}}
  --%test
  --%disabled(nested context disabled)
  PROCEDURE nested_context_disabled;

  --%endcontext
  --%endcontext

  -- Noncompliant@+2 {{Fix or remove this disabled unit test.}}
  --%test
  --%disabled(first reason)
  --%disabled(second reason)
  PROCEDURE duplicate_disabled;

  --%disabled(Suite temporarily disabled)
  --%context(Disabled context)
  --%disabled(Context temporarily disabled)

  --%test
  PROCEDURE enabled_test;

  --%disabled
  PROCEDURE helper;

  PROCEDURE inline_annotation;

  -- ordinary comment containing the word disabled
  -- this is not a %disabled annotation
  PROCEDURE ordinary_comment;
END test_pkg;

CREATE PACKAGE no_suite AS
  --%test
  --%disabled
  PROCEDURE helper_test;
END no_suite;

CREATE PACKAGE BODY test_pkg AS
  --%test
  --%disabled
  PROCEDURE body_test IS
  BEGIN
    NULL;
  END;
END test_pkg;

CREATE PACKAGE function_pkg AS
  --%suite

  --%test
  --%disabled
  FUNCTION invalid_test RETURN NUMBER;

  PROCEDURE inline_disabled; --%disabled
END function_pkg;
