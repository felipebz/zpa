begin
  -- noncompliant code
  var := (foo = null); -- Noncompliant {{Fix this comparison or change to "IS NULL".}} [[sc=11;ec=21]]
  var := (foo = ''); -- Noncompliant [[sc=11;ec=19]]
  var := (foo <> null); -- Noncompliant {{Fix this comparison or change to "IS NOT NULL".}} [[sc=11;ec=22]]
  var := (foo <> ''); -- Noncompliant [[sc=11;ec=20]]
  
  -- valid code
  var := (foo is null);
  var := (foo is not null);
  var := (foo = 'x');
  var := (foo <> 'x');
  var := (foo = 1);
end;