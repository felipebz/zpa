begin
  insert into tab values (1); -- violation
  
  insert into tab (col) values (1);
end;