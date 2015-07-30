begin
  var := nvl(x, null); -- violation
  var := nvl(null, x); -- violation
  var := nvl(x, ''); -- violation
  var := nvl('', x); -- violation
  
  var := nvl(x, y);
  var := nvl(x, 'y');
  var := func(x, null);
  var := pack.func(x, null);
end;