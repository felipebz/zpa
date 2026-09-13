CREATE PACKAGE invalid_args AS
  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%suitepath

  PROCEDURE suitepath_missing;

  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%suitepath()

  PROCEDURE suitepath_empty;

  -- Noncompliant@+1 {{Invalid value "foo bar" for this utPLSQL annotation.}}
  --%suitepath(foo bar)

  PROCEDURE suitepath_spaces;

  -- Noncompliant@+1 {{Invalid value "foo..bar" for this utPLSQL annotation.}}
  --%suitepath(foo..bar)

  PROCEDURE suitepath_empty_component;

  --%suitepath(foo.bar)

  PROCEDURE suitepath_valid;

  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%rollback

  PROCEDURE rollback_missing;

  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%rollback()

  PROCEDURE rollback_empty;

  -- Noncompliant@+1 {{Invalid value "transactional" for this utPLSQL annotation.}}
  --%rollback(transactional)

  PROCEDURE rollback_invalid;

  --%rollback(AUTO)

  PROCEDURE rollback_auto;

  --%rollback(manual)

  PROCEDURE rollback_manual;

  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%tags

  PROCEDURE tags_missing;

  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%tags()

  PROCEDURE tags_empty;

  -- Noncompliant@+1 {{Invalid value "-fast" for this utPLSQL annotation.}}
  --%tags(-fast)

  PROCEDURE tags_dash;

  -- Noncompliant@+1 {{Invalid value "a" for this utPLSQL annotation.}}
  --%tags(a)

  PROCEDURE tags_short;

  -- Noncompliant@+1 {{Invalid value "bad tag" for this utPLSQL annotation.}}
  --%tags(bad tag)

  PROCEDURE tags_space;

  -- Noncompliant@+1 {{Invalid value "foo&bar" for this utPLSQL annotation.}}
  --%tags(foo&bar)

  PROCEDURE tags_ampersand;

  -- Noncompliant@+1 {{Invalid value "foo|bar" for this utPLSQL annotation.}}
  --%tags(foo|bar)

  PROCEDURE tags_pipe;

  -- Noncompliant@+1 {{Invalid value "!foo" for this utPLSQL annotation.}}
  --%tags(!foo)

  PROCEDURE tags_not;

  -- Noncompliant@+1 {{Invalid value "foo(bar)" for this utPLSQL annotation.}}
  --%tags(foo(bar))

  PROCEDURE tags_parentheses;

  -- Noncompliant@+1 {{Invalid value "none" for this utPLSQL annotation.}}
  --%tags(none)

  PROCEDURE tags_none;

  -- Noncompliant@+1 {{Invalid value "any" for this utPLSQL annotation.}}
  --%tags(any)

  PROCEDURE tags_any;

  --%tags(fast)

  PROCEDURE tags_short_valid;

  --%tags(integration-test)

  PROCEDURE tags_internal_dash;

  --%tags(test_tag)

  PROCEDURE tags_underscore;

  --%tags(None)

  PROCEDURE tags_case_sensitive_none;

  --%tags(Any)

  PROCEDURE tags_case_sensitive_any;

  --%tags(fast, slow)

  PROCEDURE tags_valid;

  -- Noncompliant@+2 {{This utPLSQL annotation requires an argument.}}
  --%test
  --%throws
  PROCEDURE throws_missing;

  -- Noncompliant@+2 {{This utPLSQL annotation requires an argument.}}
  --%test
  --%throws()
  PROCEDURE throws_empty;

  --%test
  --%throws(-20001)
  PROCEDURE throws_number;

  --%test
  --%throws(no_such_resolution_required)
  PROCEDURE throws_identifier;

  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%displayname

  PROCEDURE package_displayname_missing;

  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%displayname()

  PROCEDURE package_displayname_empty;

  --%displayname(Package description)

  PROCEDURE package_displayname_valid;

  --%displayname
  PROCEDURE procedure_displayname_optional;

  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%beforeall

  PROCEDURE package_beforeall_missing;

  --%beforeall(setup_proc)

  PROCEDURE package_beforeall_valid;

  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%afterall

  PROCEDURE package_afterall_missing;

  --%afterall(teardown_proc)

  PROCEDURE package_afterall_valid;

  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%beforeeach

  PROCEDURE package_beforeeach_missing;

  --%beforeeach(setup_each)

  PROCEDURE package_beforeeach_valid;

  -- Noncompliant@+1 {{This utPLSQL annotation requires an argument.}}
  --%aftereach

  PROCEDURE package_aftereach_missing;

  --%aftereach(teardown_each)

  PROCEDURE package_aftereach_valid;

  -- Noncompliant@+2 {{This utPLSQL annotation requires an argument.}}
  --%test
  --%beforetest
  PROCEDURE beforetest_missing;

  --%test
  --%beforetest(setup_for_test)
  PROCEDURE beforetest_valid;

  -- Noncompliant@+2 {{This utPLSQL annotation requires an argument.}}
  --%test
  --%aftertest()
  PROCEDURE aftertest_empty;

  --%test
  --%aftertest(teardown_for_test)
  PROCEDURE aftertest_valid;

  --%tags(-fast)
  PROCEDURE helper_tags;

  --%rollback(invalid)
  PROCEDURE helper_rollback;

  --%throws
  PROCEDURE helper_throws;

  --%beforetest
  PROCEDURE helper_beforetest;

  --%aftertest
  PROCEDURE helper_aftertest;

  --%displayname(unclosed
  PROCEDURE helper_displayname;

  --%disabled(unclosed
  PROCEDURE helper_disabled;

  --%test(unclosed

  PROCEDURE floating_test;

  -- Noncompliant@+2 {{Invalid value "-fast" for this utPLSQL annotation.}}
  --%test
  --%tags(-fast)
  PROCEDURE actual_test_tags;

  -- Noncompliant@+2 {{Invalid value "invalid" for this utPLSQL annotation.}}
  --%test
  --%rollback(invalid)
  PROCEDURE actual_test_rollback;

  -- Noncompliant@+2 {{This utPLSQL annotation requires an argument.}}
  --%test
  --%throws
  PROCEDURE actual_test_throws;

  -- Noncompliant@+2 {{This utPLSQL annotation requires an argument.}}
  --%test
  --%beforetest
  PROCEDURE actual_test_beforetest;

  -- Noncompliant@+2 {{This utPLSQL annotation requires an argument.}}
  --%test
  --%aftertest
  PROCEDURE actual_test_aftertest;

  -- Noncompliant@+2 {{This utPLSQL annotation argument is malformed and will be ignored.}}
  --%test
  --%displayname(unclosed
  PROCEDURE actual_test_displayname;

  -- Noncompliant@+2 {{This utPLSQL annotation argument is malformed and will be ignored.}}
  --%test
  --%disabled(unclosed
  PROCEDURE actual_test_disabled;

  -- Noncompliant@+1 {{This utPLSQL annotation argument is malformed and will be ignored.}}
  --%suite Description without brackets

  PROCEDURE malformed_suite;

  -- Noncompliant@+1 {{This utPLSQL annotation argument is malformed and will be ignored.}}
  --%test(unclosed
  PROCEDURE malformed_test;

  --%suite

  --%test
  PROCEDURE optional_test;

  --%test()
  PROCEDURE optional_empty_test;

  --%context

  PROCEDURE context_optional;

  --%disabled()
  PROCEDURE disabled_optional;

  --%future_annotation(foo)
  PROCEDURE unknown_annotation;

  --%future_annotation(unclosed
  PROCEDURE unknown_malformed_annotation;
END invalid_args;

CREATE PACKAGE BODY invalid_args AS
  PROCEDURE suitepath_missing IS BEGIN NULL; END;
  PROCEDURE suitepath_empty IS BEGIN NULL; END;
  PROCEDURE suitepath_spaces IS BEGIN NULL; END;
  PROCEDURE suitepath_empty_component IS BEGIN NULL; END;
  PROCEDURE suitepath_valid IS BEGIN NULL; END;
  PROCEDURE rollback_missing IS BEGIN NULL; END;
  PROCEDURE rollback_empty IS BEGIN NULL; END;
  PROCEDURE rollback_invalid IS BEGIN NULL; END;
  PROCEDURE rollback_auto IS BEGIN NULL; END;
  PROCEDURE rollback_manual IS BEGIN NULL; END;
  PROCEDURE tags_missing IS BEGIN NULL; END;
  PROCEDURE tags_empty IS BEGIN NULL; END;
  PROCEDURE tags_dash IS BEGIN NULL; END;
  PROCEDURE tags_short IS BEGIN NULL; END;
  PROCEDURE tags_space IS BEGIN NULL; END;
  PROCEDURE tags_ampersand IS BEGIN NULL; END;
  PROCEDURE tags_pipe IS BEGIN NULL; END;
  PROCEDURE tags_not IS BEGIN NULL; END;
  PROCEDURE tags_parentheses IS BEGIN NULL; END;
  PROCEDURE tags_none IS BEGIN NULL; END;
  PROCEDURE tags_any IS BEGIN NULL; END;
  PROCEDURE tags_short_valid IS BEGIN NULL; END;
  PROCEDURE tags_internal_dash IS BEGIN NULL; END;
  PROCEDURE tags_underscore IS BEGIN NULL; END;
  PROCEDURE tags_case_sensitive_none IS BEGIN NULL; END;
  PROCEDURE tags_case_sensitive_any IS BEGIN NULL; END;
  PROCEDURE tags_valid IS BEGIN NULL; END;
  PROCEDURE throws_missing IS BEGIN NULL; END;
  PROCEDURE throws_empty IS BEGIN NULL; END;
  PROCEDURE throws_number IS BEGIN NULL; END;
  PROCEDURE throws_identifier IS BEGIN NULL; END;
  PROCEDURE package_displayname_missing IS BEGIN NULL; END;
  PROCEDURE package_displayname_empty IS BEGIN NULL; END;
  PROCEDURE package_displayname_valid IS BEGIN NULL; END;
  PROCEDURE procedure_displayname_optional IS BEGIN NULL; END;
  PROCEDURE package_beforeall_missing IS BEGIN NULL; END;
  PROCEDURE package_beforeall_valid IS BEGIN NULL; END;
  PROCEDURE package_afterall_missing IS BEGIN NULL; END;
  PROCEDURE package_afterall_valid IS BEGIN NULL; END;
  PROCEDURE package_beforeeach_missing IS BEGIN NULL; END;
  PROCEDURE package_beforeeach_valid IS BEGIN NULL; END;
  PROCEDURE package_aftereach_missing IS BEGIN NULL; END;
  PROCEDURE package_aftereach_valid IS BEGIN NULL; END;
  PROCEDURE beforetest_missing IS BEGIN NULL; END;
  PROCEDURE beforetest_valid IS BEGIN NULL; END;
  PROCEDURE aftertest_empty IS BEGIN NULL; END;
  PROCEDURE aftertest_valid IS BEGIN NULL; END;
  PROCEDURE helper_tags IS BEGIN NULL; END;
  PROCEDURE helper_rollback IS BEGIN NULL; END;
  PROCEDURE helper_throws IS BEGIN NULL; END;
  PROCEDURE helper_beforetest IS BEGIN NULL; END;
  PROCEDURE helper_aftertest IS BEGIN NULL; END;
  PROCEDURE helper_displayname IS BEGIN NULL; END;
  PROCEDURE helper_disabled IS BEGIN NULL; END;
  PROCEDURE floating_test IS BEGIN NULL; END;
  PROCEDURE actual_test_tags IS BEGIN NULL; END;
  PROCEDURE actual_test_rollback IS BEGIN NULL; END;
  PROCEDURE actual_test_throws IS BEGIN NULL; END;
  PROCEDURE actual_test_beforetest IS BEGIN NULL; END;
  PROCEDURE actual_test_aftertest IS BEGIN NULL; END;
  PROCEDURE actual_test_displayname IS BEGIN NULL; END;
  PROCEDURE actual_test_disabled IS BEGIN NULL; END;
  PROCEDURE malformed_suite IS BEGIN NULL; END;
  PROCEDURE malformed_test IS BEGIN NULL; END;
  PROCEDURE optional_test IS BEGIN NULL; END;
  PROCEDURE optional_empty_test IS BEGIN NULL; END;
  PROCEDURE context_optional IS BEGIN NULL; END;
  PROCEDURE disabled_optional IS BEGIN NULL; END;
  PROCEDURE unknown_annotation IS BEGIN NULL; END;
  PROCEDURE unknown_malformed_annotation IS BEGIN NULL; END;
END invalid_args;
