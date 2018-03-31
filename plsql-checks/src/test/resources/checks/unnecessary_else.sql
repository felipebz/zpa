begin
  if (foo = bar) then
    return;
  else -- Noncompliant {{This ELSE can be replaced by an END IF. When the corresponding IF is executed, the code execution will be stopped. Either way, the code within this ELSE will always run, regardless of the ELSE block.}}
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

  if (foo = bar) then
    raise_application_error(-20999, 'Custom error message');
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