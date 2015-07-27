begin
  -- noncompliant code
  var := (foo = null);
  var := (foo <> null);
  var := (foo = '');
  var := (foo <> '');
  
  -- valid code
  var := (foo is null);
  var := (foo is not null);
  var := (foo = 'x');
  var := (foo <> 'x');
  var := (foo = 1);
end;