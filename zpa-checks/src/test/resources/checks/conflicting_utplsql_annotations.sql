CREATE PACKAGE test_pkg AS
  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "test" on the same declaration.}}
  -- %beforeall(setup)
  -- %test
  PROCEDURE suite_with_test;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" on the same declaration.}}
  -- %test
  -- %beforeall(setup)
  PROCEDURE test_beforeall_with_argument;

  -- Noncompliant@+1 {{This package-level utPLSQL annotation is ineffective when attached to a subprogram.}}
  -- %suite
  -- %test
  PROCEDURE suite_before_test;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" on the same declaration.}}
  -- %test(first)
  -- %TEST(duplicate)
  PROCEDURE duplicate_test;

  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "test" on the same declaration.}}
  -- %beforeall
  -- %test
  PROCEDURE test_beforeall;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" on the same declaration.}}
  -- %test
  -- %beforeall
  PROCEDURE beforeall_test;

  -- Noncompliant@+1 {{This package-level utPLSQL annotation is ineffective when attached to a subprogram.}}
  -- %suite
  PROCEDURE suite_on_procedure;

  -- %tags(one)
  -- %tags(two)
  -- %beforetest(setup_one)
  -- %beforetest(setup_two)
  PROCEDURE repeatable_annotations;

  -- %displayname(Valid procedure)
  PROCEDURE valid_procedure;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "afterall" on the same declaration.}}
  -- %afterall
  -- %afterall
  PROCEDURE duplicate_afterall;

  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "test" on the same declaration.}}
  -- %afterall
  -- %test
  PROCEDURE afterall_test;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" on the same declaration.}}
  -- %test
  -- %afterall
  PROCEDURE test_afterall;

  -- %beforeall(setup_one)
  -- %beforeall(setup_two)

  -- %test
  PROCEDURE package_level_hooks;

  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "test" on the same declaration.}}
  -- %beforeeach
  -- %test
  PROCEDURE beforeeach_test;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" on the same declaration.}}
  -- %test
  -- %beforeeach
  PROCEDURE test_beforeeach;

  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "test" on the same declaration.}}
  -- %aftereach
  -- %test
  PROCEDURE aftereach_test;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" on the same declaration.}}
  -- %test
  -- %aftereach
  PROCEDURE test_aftereach;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "displayname" on the same declaration.}}
  -- %displayname(first)
  -- %displayname(second)
  PROCEDURE duplicate_displayname;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "rollback" on the same declaration.}}
  -- %rollback(auto)
  -- %rollback(manual)
  PROCEDURE duplicate_rollback;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "beforeeach" on the same declaration.}}
  -- %beforeeach
  -- %beforeeach
  PROCEDURE duplicate_beforeeach;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "suite" on the same declaration.}}
  -- %suite(first)
  -- %suite(second)

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "suitepath" on the same declaration.}}
  -- %suitepath(first)
  -- %suitepath(second)

  -- %context(first_context)
  -- %displayname(First context)
  -- %rollback(auto)
  -- %endcontext

  -- %context(second_context)
  -- %displayname(Second context)
  -- %rollback(manual)
  -- %endcontext
END test_pkg;

CREATE PACKAGE BODY test_pkg AS
  PROCEDURE duplicate_test IS BEGIN NULL; END;
  PROCEDURE test_beforeall IS BEGIN NULL; END;
  PROCEDURE beforeall_test IS BEGIN NULL; END;
  PROCEDURE suite_on_procedure IS BEGIN NULL; END;
  PROCEDURE repeatable_annotations IS BEGIN NULL; END;
  PROCEDURE valid_procedure IS BEGIN NULL; END;
  PROCEDURE duplicate_afterall IS BEGIN NULL; END;
  PROCEDURE afterall_test IS BEGIN NULL; END;
  PROCEDURE test_afterall IS BEGIN NULL; END;
  PROCEDURE package_level_hooks IS BEGIN NULL; END;
  PROCEDURE test_beforeeach IS BEGIN NULL; END;
  PROCEDURE aftereach_test IS BEGIN NULL; END;
  PROCEDURE duplicate_displayname IS BEGIN NULL; END;
  PROCEDURE duplicate_rollback IS BEGIN NULL; END;
  PROCEDURE duplicate_beforeeach IS BEGIN NULL; END;
END test_pkg;
