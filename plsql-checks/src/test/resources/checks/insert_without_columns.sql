declare
  var tab%rowtype;
begin
  insert into tab values (1); -- Noncompliant {{Specify the columns in this INSERT.}}
  
  insert into tab (col) values (1);

  insert into tab values var;

  for r_t in (select * from mytable) loop
      insert into mytable values r_t;
  end loop;

end;
