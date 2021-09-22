begin
  var := ''; -- Noncompliant {{Replace this empty string by NULL.}}
  
  -- correct
  var := ' ';
  var := 'x';
  var := null;
end;