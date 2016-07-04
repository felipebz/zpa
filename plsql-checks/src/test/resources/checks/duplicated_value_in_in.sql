begin
  var := (x in (1, 1, 2)); -- Noncompliant {{Remove or fix the duplicated value "1" in the IN condition.}} [[secondary=2]]
--                 ^
  
  select col
    into var
    from tab
   where col in (x, y, x); -- Noncompliant {{Remove or fix the duplicated value "x" in the IN condition.}} [[secondary=8]]
--                     ^
   
  -- correct
  var := (x in (1, 2, 3));
end;