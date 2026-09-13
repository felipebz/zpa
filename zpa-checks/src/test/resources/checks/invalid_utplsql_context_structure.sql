CREATE PACKAGE missing_context AS
  --%suite

  -- Noncompliant@+1 {{This utPLSQL context has no matching "%endcontext".}}
  --%context(Unclosed context)

  --%test
  PROCEDURE test_in_unclosed_context;
END missing_context;

CREATE PACKAGE context_names AS
  --%suite

  -- Noncompliant@+1 {{This "%name" annotation is outside a utPLSQL context and is ignored.}}
  --%name(outside_context)

  --%context(First context)
  --%name(shared)
  --%tags(valid_tag)
  --%endcontext

  --%context(Second context)
  -- Noncompliant@+1 {{This context name duplicates a sibling context name "shared". utPLSQL will use an automatic name instead.}}
  --%name(shared)
  --%endcontext

  --%context(Invalid context)

  -- Noncompliant@+1 {{Invalid context name "invalid.name". utPLSQL will use an automatic name instead.}}
  --%name(invalid.name)
  --%endcontext

  --%context(Invalid dash context)
  -- Noncompliant@+1 {{Invalid context name "invalid-name". utPLSQL will use an automatic name instead.}}
  --%name(invalid-name)
  --%endcontext

  --%context(Invalid space context)
  -- Noncompliant@+1 {{Invalid context name "invalid name". utPLSQL will use an automatic name instead.}}
  --%name(invalid name)
  --%endcontext

  --%context(Valid context name)
  --%name(valid_name$#)
  --%endcontext

  --%context(Accented context name)
  --%name(emissão)
  --%endcontext

  --%context(Another accented context name)
  --%name(ação_1)
  --%endcontext

  --%context(Non-Latin context name)
  --%name(δοκιμή_1)
  --%endcontext

  --%context(Default context name)
  --%name
  --%endcontext

  --%context(Duplicate name context)
  --%name(first_name)
  -- Noncompliant@+1 {{This "%name" annotation is duplicated in the same context.}}
  --%name(second_name)
  --%endcontext

  --%context(Invalid first name context)
  -- Noncompliant@+1 {{Invalid context name "invalid.name". utPLSQL will use an automatic name instead.}}
  --%name(invalid.name)
  -- Noncompliant@+1 {{This "%name" annotation is duplicated in the same context.}}
  --%name(valid_after_invalid)
  --%endcontext

  --%context(Bare first name context)
  --%name
  -- Noncompliant@+1 {{This "%name" annotation is duplicated in the same context.}}
  --%name(valid_after_bare)
  --%endcontext

  --%context(Empty first name context)
  --%name()
  -- Noncompliant@+1 {{This "%name" annotation is duplicated in the same context.}}
  --%name(valid_after_empty)
  --%endcontext

  --%context(Malformed first name context)
  --%name(unclosed
  -- Noncompliant@+1 {{This "%name" annotation is duplicated in the same context.}}
  --%name(valid_after_malformed)
  --%endcontext

  --%context(Effective sibling name)
  --%name(actual_name)
  -- Noncompliant@+1 {{This "%name" annotation is duplicated in the same context.}}
  --%name(shared_late)
  --%endcontext

  --%context(Later sibling name)
  --%name(shared_late)
  --%endcontext

  --%context(Case-sensitive first name)
  --%name(case_sensitive_name)
  --%endcontext

  --%context(Case-sensitive second name)
  --%name(CASE_SENSITIVE_NAME)
  --%endcontext

  --%context(Late parent name)
  --%name(valid_late_parent)
  --%context(Child context)
  --%endcontext
  -- Noncompliant@+1 {{This "%name" annotation appears after a nested context has started and is ignored.}}
  --%name(too_late)
  --%endcontext

  --%context(Nested parent A)
  --%name(parent_a)
  --%context(Child A)
  --%name(shared)

  --%test
  PROCEDURE test_in_child_a;
  --%endcontext
  --%endcontext

  --%context(Nested parent B)
  --%name(parent_b)
  --%context(Child B)
  --%name(shared)

  --%test
  PROCEDURE test_in_child_b;
  --%endcontext
  --%endcontext

  -- Noncompliant@+1 {{This "%endcontext" has no matching "%context".}}
  --%endcontext

  --%context(Procedure placement)
  --%test
  PROCEDURE procedure_context_marker;

  --%context(Empty explicit name)

  --%name()
  --%endcontext
END context_names;

CREATE PACKAGE BODY context_names AS
  PROCEDURE test_in_child_a IS BEGIN NULL; END;
  PROCEDURE test_in_child_b IS BEGIN NULL; END;
  PROCEDURE procedure_context_marker IS BEGIN NULL; END;

  --%context(Body context)
  --%endcontext
END context_names;

CREATE PACKAGE no_suite AS
  --%context(Not built)
  --%name(invalid.name)
END no_suite;

CREATE PACKAGE nested_missing_context AS
  --%suite
  -- Noncompliant@+1 {{This utPLSQL context has no matching "%endcontext".}}
  --%context(Outer context)
  --%context(Inner context)
  --%endcontext
END nested_missing_context;

CREATE PACKAGE fully_unclosed_nested AS
  --%suite
  -- Noncompliant@+1 {{This utPLSQL context has no matching "%endcontext".}}
  --%context(Outer context)
  -- Noncompliant@+1 {{This "%context" annotation is duplicated in the same context.}}
  --%context(Inner context)
  -- package-level boundary before the test annotation
  --%test
  PROCEDURE test_without_endcontext;
END fully_unclosed_nested;

CREATE PACKAGE earlier_end_then_unclosed AS
  --%suite
  --%context(Already closed)
  --%endcontext

  -- Noncompliant@+1 {{This utPLSQL context has no matching "%endcontext".}}
  --%context(Outer context)
  -- Noncompliant@+1 {{This "%context" annotation is duplicated in the same context.}}
  --%context(Inner context)
  -- package-level boundary before the test annotation
  --%test
  PROCEDURE test_after_earlier_endcontext;
END earlier_end_then_unclosed;
