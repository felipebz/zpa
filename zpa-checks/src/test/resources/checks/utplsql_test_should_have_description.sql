CREATE PACKAGE described_suite AS
  --%suite

  -- Noncompliant@+1 {{Add a description to this utPLSQL test.}}
  --%test
  PROCEDURE no_description;

  -- Noncompliant@+1 {{Add a description to this utPLSQL test.}}
  --%test()
  PROCEDURE empty_description;

  -- Noncompliant@+1 {{Add a description to this utPLSQL test.}}
  --%test
  --%displayname()
  PROCEDURE empty_override;

  -- Noncompliant@+1 {{Add a description to this utPLSQL test.}}
  --%test(Original description)
  --%displayname()
  PROCEDURE empty_displayname_overrides_test;

  --%test(Has a description)
  PROCEDURE described;

  --%test
  FUNCTION undescribed_function RETURN NUMBER;

  --%test(Has a description)
  FUNCTION described_function RETURN NUMBER;

  --%test
  --%displayname(Function description)
  FUNCTION function_with_displayname RETURN NUMBER;

  --%test
  --%displayname(Has a description)
  PROCEDURE described_by_displayname;

  --%test(Test description)
  --%displayname(Override description)
  PROCEDURE described_by_override;

  -- Noncompliant@+1 {{Add a description to this utPLSQL test.}}
  --%test
  --%test(Ignored description)
  PROCEDURE duplicate_test;

  -- Noncompliant@+1 {{Add a description to this utPLSQL test.}}
  --%test
  --%displayname()
  --%displayname(Ignored description)
  PROCEDURE duplicate_displayname;

  --%test(Kept description)
  --%displayname(Ignored empty duplicate)
  --%displayname()
  PROCEDURE duplicate_displayname_kept;

  --%test(unclosed
  PROCEDURE malformed_test;

  --%test(Valid test description)
  --%displayname(unclosed
  PROCEDURE malformed_displayname;

  --%context(Customer creation)

  -- Noncompliant@+1 {{Add a description to this utPLSQL test.}}
  --%test
  PROCEDURE context_test;

  --%context(Inner context)

  -- Noncompliant@+1 {{Add a description to this utPLSQL test.}}
  --%test
  PROCEDURE nested_context_test;

  --%endcontext
  --%endcontext

  -- Noncompliant@+1 {{Add a description to this utPLSQL test.}}
  --%test
  --%disabled(temporarily broken)
  PROCEDURE disabled_test;

  PROCEDURE helper;
END described_suite;

CREATE PACKAGE no_suite AS
  --%test
  PROCEDURE helper_test;
END no_suite;

CREATE PACKAGE BODY described_suite AS
  FUNCTION undescribed_function RETURN NUMBER IS BEGIN RETURN 1; END;
  FUNCTION described_function RETURN NUMBER IS BEGIN RETURN 1; END;
  FUNCTION function_with_displayname RETURN NUMBER IS BEGIN RETURN 1; END;
  --%test
  PROCEDURE body_test IS BEGIN NULL; END;
END described_suite;
