begin
  var := 'a'||null; -- violation
  var := 'a'||''; -- violation
  var := 'a'||'1'; -- violation
end;