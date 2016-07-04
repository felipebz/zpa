begin
  -- noncompliant code
  var := (foo = null); -- Noncompliant {{Fix this comparison or change to "IS NULL".}}
--        ^^^^^^^^^^

  var := (foo = ''); -- Noncompliant
--        ^^^^^^^^

  var := (foo <> null); -- Noncompliant {{Fix this comparison or change to "IS NOT NULL".}}
--        ^^^^^^^^^^^

  var := (foo <> ''); -- Noncompliant
--        ^^^^^^^^^
  
  -- valid code
  var := (foo is null);
  var := (foo is not null);
  var := (foo = 'x');
  var := (foo <> 'x');
  var := (foo = 1);
end;