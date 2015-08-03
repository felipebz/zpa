begin
  select name
    into var
    from user
   where rownum <= 1 -- violation
   order by date;
   
  select name
    into var
    from user
   where (rownum <= 1) -- violation
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
end;