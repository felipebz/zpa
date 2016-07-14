begin
  select name
    into var
    from user
   where rownum <= 1 -- Noncompliant {{Move this ROWNUM comparation to a more external level to guarantee the ordering.}}
   order by date;
   
  select name
    into var
    from user
   where (rownum <= 1) -- Noncompliant
   order by date;
   
  -- valid code
  select name
    into var
    from user
   where func(rownum) = filter
   order by date;
   
  select name
    into var
    from user
   where rownum = 1;
   
  select name
    into var
    from user
   order by date;
   
  select name
    into var
    from user
   where foo = (select foo from bar where rownum = 1)
   order by date;
end;