begin
  var := (x = true); -- violation
  var := (x != true); -- violation
  var := (x = false); -- violation
  var := (x != false); -- violation
  var := (true = true); -- violation
  
  var := (x = y);
end;