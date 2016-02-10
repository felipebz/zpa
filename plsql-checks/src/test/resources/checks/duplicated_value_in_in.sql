begin
  var := (x in (1, 1, 2)); -- Noncompliant {{Remove or fix the duplicated value "1" in the IN condition.}} [[sc=20;ec=21;secondary=2]]
  
  select col
    into var
    from tab
   where col in (x, y, x); -- Noncompliant {{Remove or fix the duplicated value "x" in the IN condition.}} [[sc=24;ec=25;secondary=7]]
   
  -- correct
  var := (x in (1, 2, 3));
end;