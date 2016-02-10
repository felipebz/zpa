begin
  insert into tab values (1); -- Noncompliant {{Specify the columns in this INSERT.}}
  
  insert into tab (col) values (1);
end;