begin
  if foo then
    null;
  elsif foo then -- Noncompliant {{This code can not be reached because the condition duplicates a previous condition in the same sequence of "if/else if" statements.}}
    null;
  end if;
  
  if foo then
    null;
  elsif bar then
    null;
  elsif bar then -- Noncompliant
    null;
  elsif (bar) then -- Noncompliant
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