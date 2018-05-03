begin
  if x then
    var := 1;
  elsif y then
    var := 1; -- Noncompliant {{Either merge this branch with the identical one on line 3 or change one of the implementations.}} [[secondary=3]]
--  ^^^^^^^^^
  end if;
  
  if x then
    var := 1;
  else
    var := 1; -- Noncompliant [[secondary=10]]
--  ^^^^^^^^^
  end if;
  
  if x then
    var := 1;
  else
    var := (1); -- Noncompliant [[secondary=17]]
--  ^^^^^^^^^^^
  end if;

  -- compliant
  if x then
    var := 1;
  elsif y then
    var := 2;
  elsif z then
    var := 1;
  end if;
end;
