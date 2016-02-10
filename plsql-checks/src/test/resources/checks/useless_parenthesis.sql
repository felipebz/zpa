begin
  var := ((x = 1)); -- Noncompliant {{Remove those useless parenthesis.}} [[sc=11;ec=18]]
  
  -- valid
  var := (x = 1);
  var := ((x = 1) or (y = 2));
end;