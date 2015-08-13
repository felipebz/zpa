begin
  var := (x = 1 or x = 2 and y = 3);
  var := (x = 1 or x = 2 and y = 3 or x = 4 and y = 5);
  
  select 1
    into var
    from dual
   where x = 1
      or x = 2
     and y = 3;
     
  -- correct
  var := (x = 1 or (x = 2 and y = 3));
  var := (x = 1 or (x = 2 and y = 3) or (x = 4 and y = 5));
  
  select 1
    into var
    from dual
   where x = 1
      or (x = 2
     and y = 3);
end;