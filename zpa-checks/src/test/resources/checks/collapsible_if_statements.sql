begin
  -- can be collapsed
  if x = 1 then
    if y = 2 then -- Noncompliant {{This IF statement can be merged with the enclosing one.}}
      null;
    end if;
  end if;
  
  if x = 1 then
    if y = 2 then -- Noncompliant
      if z = 3 then -- Noncompliant
        null;
      end if;
    end if;
  end if;
  
  -- cannot be collapsed
  -- if with elsif
  if x = 1 then
    if y = 2 then
      null;
    end if;
  elsif x = 2 then
    null;
  end if;
  
  -- if with else
  if x = 1 then
    if y = 2 then
      null;
    end if;
  else
    null;
  end if;
  
  -- if with more than 1 statement
  if x = 1 then
    null;
    
    if y = 2 then
      null;
    end if;
  end if;
  
  if x = 1 then
    if y = 2 then
      null;
    end if;
    
    null;
  end if;
  
  -- internal if has elsif/else
  if x = 1 then
    if y = 2 then
      null;
    elsif y = 3 then
      null;
    else
      null;
    end if;
  elsif x = 2 then
    null;
  end if;
end;
/