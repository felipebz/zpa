begin
  var := (a = a); -- Noncompliant {{Identical expressions on both sides of operator "=".}} [[secondary=2]]
--        ^
  
  select 1
    into var
    from tab
   where tab.col = tab.col; -- Noncompliant [[secondary=8]]
--       ^^^^^^^
  
  var := not a = a; -- Noncompliant [[secondary=11]]
--           ^

  var := (a) = a; -- Noncompliant [[secondary=14]]
--       ^^^

  var := (a + b + (c)) = a + (b) + c; -- Noncompliant [[secondary=17]]
--       ^^^^^^^^^^^^^
  
  -- valid
  var := (a = b);
end;