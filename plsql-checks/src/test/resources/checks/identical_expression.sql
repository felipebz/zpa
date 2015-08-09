begin
  var := (a = a); -- violation
  
  select 1
    into var
    from tab
   where tab.col = tab.col; -- violation
  
  -- valid
  var := (a = b);
end;