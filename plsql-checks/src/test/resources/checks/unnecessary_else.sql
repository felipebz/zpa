begin
  if (foo = bar) then
    return;
  else -- Noncompliant {{The content of this ELSE clause will always be executed, so it doesn't need to be wrapped in an ELSE.}}
    foo := null;
  end if;
  
  if (foo = bar) then
    exit;
  else -- Noncompliant
    foo := null;
  end if;
  
  if (foo = bar) then
    continue;
  else -- Noncompliant
    foo := null;
  end if;
  
  if (foo = bar) then
    raise;
  else -- Noncompliant
    foo := null;
  end if;
end;


-- correct
begin
  if (foo = bar) then
    get_data;
  else
    foo := null;
  end if;
  
  if (foo = bar) then
    exit when baz;
  else
    foo := null;
  end if;
  
  if (foo = bar) then
    continue when baz;
  else
    foo := null;
  end if;
  
  if (foo = bar) then
    return;
  elsif baz then
    null;
  else
    foo := null;
  end if;
end;