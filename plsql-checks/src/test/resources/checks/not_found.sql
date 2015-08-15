begin
  var := not cur%found; -- violation
  
  var := cur%notfound;
  var := not cur.found;
  var := cur.found;
  var := cur%found;
end;