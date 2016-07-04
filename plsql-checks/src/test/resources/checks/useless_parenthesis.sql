begin
  var := ((x = 1)); -- Noncompliant {{Remove those useless parenthesis.}}
--        ^^^^^^^
  
  -- valid
  var := (x = 1);
  var := ((x = 1) or (y = 2));
end;