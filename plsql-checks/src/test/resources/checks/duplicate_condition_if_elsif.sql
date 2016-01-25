begin
  if foo then
    null;
  elsif foo then -- violation
    null;
  end if;
  
  if foo then
    null;
  elsif bar then
    null;
  elsif bar then -- violation
    null;
  end if;
  
  -- correct
  if foo then
    null;
  end if;
  
  if foo then
    null;
  elsif bar then
    null;
  elsif baz then
    null;
  end if;
end;
/