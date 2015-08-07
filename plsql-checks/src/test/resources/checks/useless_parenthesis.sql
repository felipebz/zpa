begin
  var := ((x = 1)); -- violation
  
  -- valid
  var := (x = 1);
  var := ((x = 1) or (y = 2));
end;