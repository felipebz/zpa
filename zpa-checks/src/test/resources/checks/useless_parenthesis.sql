begin
  var := ((x = 1)); -- Noncompliant {{Remove these useless parentheses.}}
--        ^^^^^^^
  
  -- valid
  var := (x = 1);
  var := ((x = 1) or (y = 2));
end;