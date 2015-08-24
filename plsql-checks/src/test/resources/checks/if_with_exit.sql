begin
  if (x = y) then
    exit; -- violation
  end if;
  
  if (x = y) then
    foo := bar;
    exit; -- ok
  end if;
  
  if (x = y) then
    exit; -- ok
    foo := bar;
  end if;
  
  if (x = y) then
    exit; -- ok
  elsif (x = z) then
    null;
  end if;
  
  if (x = y) then
    exit; -- ok
  else
    null;
  end if;
  
  loop
    exit; -- ok
  end loop;
end;