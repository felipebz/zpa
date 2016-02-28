begin
  if x then
    var := 1;
  elsif y then
    var := 1; -- Noncompliant {{Either merge this branch with the identical one on line 3 or change one of the implementations.}} [[sc=5;ec=14;secondary=3]]
  end if;
  
  if x then
    var := 1;
  else
    var := 1; -- Noncompliant [[sc=5;ec=14;secondary=9]]
  end if;
  
  if x then
    var := 1;
  else
    var := (1); -- Noncompliant [[sc=5;ec=16;secondary=15]]
  end if;
end;