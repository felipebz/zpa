begin
  var := (a = a); -- Noncompliant {{Identical expressions on both sides of operator "=".}} [[sc=11;ec=12;secondary=2]]
  
  select 1
    into var
    from tab
   where tab.col = tab.col; -- Noncompliant [[sc=10;ec=17;secondary=7]]
  
  var := not a = a; -- Noncompliant [[sc=14;ec=15;secondary=9]]
  var := (a) = a; -- Noncompliant [[sc=10;ec=13;secondary=10]]
  var := (a + b + (c)) = a + (b) + c; -- Noncompliant [[sc=10;ec=23;secondary=11]]
  
  -- valid
  var := (a = b);
end;