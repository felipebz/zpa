begin
  var := not cur%found; -- Noncompliant {{Use %NOTFOUND instead of NOT ...%FOUND.}}
  
  var := cur%notfound;
  var := not cur.found;
  var := cur.found;
  var := cur%found;
end;