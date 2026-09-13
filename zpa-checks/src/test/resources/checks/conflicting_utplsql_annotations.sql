CREATE PACKAGE test_pkg AS
  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %beforeall(setup)
  -- %test
  PROCEDURE suite_with_test;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %test
  -- %beforeall(setup)
  PROCEDURE test_beforeall_with_argument;

  -- Noncompliant@+1 {{This package-level utPLSQL annotation is ineffective when attached to a subprogram.}}
  -- %suite
  -- %test
  PROCEDURE suite_before_test;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %test(first)
  -- %TEST(duplicate)
  PROCEDURE duplicate_test;

  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %beforeall
  -- %test
  PROCEDURE test_beforeall;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %test
  -- %beforeall
  PROCEDURE beforeall_test;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %test
  -- %beforeall(owner.pkg.setup
  PROCEDURE test_beforeall_malformed;

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

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "afterall" in the same utPLSQL scope.}}
  -- %afterall
  -- %afterall
  PROCEDURE duplicate_afterall;

  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %afterall
  -- %test
  PROCEDURE afterall_test;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %test
  -- %afterall
  PROCEDURE test_afterall;

  -- %beforeall(setup_one)
  -- %beforeall(setup_two)

  -- %test
  PROCEDURE package_level_hooks;

  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %beforeeach
  -- %test
  PROCEDURE beforeeach_test;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %test
  -- %beforeeach
  PROCEDURE test_beforeeach;

  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %aftereach
  -- %test
  PROCEDURE aftereach_test;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "test" in the same utPLSQL scope.}}
  -- %test
  -- %aftereach
  PROCEDURE test_aftereach;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "displayname" in the same utPLSQL scope.}}
  -- %displayname(first)
  -- %displayname(unclosed
  PROCEDURE duplicate_displayname;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "rollback" in the same utPLSQL scope.}}
  -- %rollback(auto)
  -- %rollback(invalid)
  PROCEDURE duplicate_rollback;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "beforeeach" in the same utPLSQL scope.}}
  -- %beforeeach
  -- %beforeeach
  PROCEDURE duplicate_beforeeach;

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "suite" in the same utPLSQL scope.}}
  -- %suite(first)
  -- %suite(second)

  -- Noncompliant@+2 {{This utPLSQL annotation conflicts with "suitepath" in the same utPLSQL scope.}}
  -- %suitepath(first)
  -- %suitepath(second)
  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "suitepath" in the same utPLSQL scope.}}
  -- %suitepath(invalid..path)
  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "suitepath" in the same utPLSQL scope.}}
  -- %suitepath(unclosed

  -- %context(first_context)
  -- %displayname(First context)
  -- %rollback(auto)
  -- %endcontext

  -- %context(second_context)
  -- %displayname(Second context)
  -- %rollback(manual)
  -- %endcontext

  -- %displayname(First root description)
  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "displayname" in the same utPLSQL scope.}}
  -- %displayname(Second root description)

  -- %rollback(auto)
  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "rollback" in the same utPLSQL scope.}}
  -- %rollback(manual)

  -- %context(single_context)
  -- %displayname(Context description)
  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "displayname" in the same utPLSQL scope.}}
  -- %displayname(Second context description)
  -- %rollback(auto)
  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "rollback" in the same utPLSQL scope.}}
  -- %rollback(manual)
  -- %tags(one)
  -- %tags(two)
  -- %beforeall(setup_one)
  -- %beforeall(setup_two)
  -- %endcontext

  -- %context(sibling_context_a)
  -- %displayname(Sibling A)
  -- %rollback(auto)
  -- %endcontext

  -- %context(sibling_context_b)
  -- %displayname(Sibling B)
  -- %rollback(manual)
  -- %endcontext

  -- %context(parent_context)
  -- %displayname(Parent)
  -- %rollback(auto)
  -- %context(child_context)
  -- %displayname(Child)
  -- %rollback(manual)
  -- %endcontext
  -- %endcontext

  -- %context(argument_duplicate_context)
  -- %displayname(first)
  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "displayname" in the same utPLSQL scope.}}
  -- %displayname(unclosed
  -- %rollback(auto)
  -- Noncompliant@+1 {{This utPLSQL annotation conflicts with "rollback" in the same utPLSQL scope.}}
  -- %rollback(invalid)
  -- %endcontext

  -- %test
  -- %displayname(first)
  -- %displayname(unclosed
  PROCEDURE duplicate_test_displayname_argument;
  -- Noncompliant@-2 {{This utPLSQL annotation conflicts with "displayname" in the same utPLSQL scope.}}
END test_pkg;

CREATE PACKAGE no_suite_conflicts AS
  -- %displayname(first)

  -- %displayname(second)

  -- %test
  -- %beforeall
  PROCEDURE helper_not_in_a_suite;
END no_suite_conflicts;

CREATE PACKAGE BODY test_pkg AS
  PROCEDURE duplicate_test IS BEGIN NULL; END;
  PROCEDURE test_beforeall IS BEGIN NULL; END;
  PROCEDURE beforeall_test IS BEGIN NULL; END;
  PROCEDURE test_beforeall_malformed IS BEGIN NULL; END;
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
  PROCEDURE duplicate_test_displayname_argument IS BEGIN NULL; END;
END test_pkg;
