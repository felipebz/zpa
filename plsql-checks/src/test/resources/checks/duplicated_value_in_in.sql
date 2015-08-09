begin
  var := (x in (1, 1, 2)); -- violation
  
  select col
    into var
    from tab
   where col in (x, y, x); -- violation
   
  -- correct
  var := (x in (1, 2, 3));
end;