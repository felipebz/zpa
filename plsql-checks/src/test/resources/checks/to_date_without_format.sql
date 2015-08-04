begin
  mydate := to_date('2015-01-01'); -- violation
  
  mydate := to_date('2015-01-01', 'rrrr-mm-dd');
end;