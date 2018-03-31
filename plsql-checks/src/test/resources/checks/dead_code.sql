begin
  return;
  
  var := 1; -- Noncompliant {{This code will never be executed.}}
end;
/

begin
  raise_application_error(-20999, 'Custom error message');
  
  var := 1; -- Noncompliant
end;
/

begin
  raise;

  var := 1; -- Noncompliant
end;
/

begin
  for i in 1..10 loop
      continue;
      var := 1; -- Noncompliant
  end loop;
end;
/

begin
  for i in 1..10 loop
      exit;
      var := 1; -- Noncompliant
  end loop;
end;
/

begin
  begin
    return;
  end;
  
  var := 1; -- Noncompliant
end;
/

begin
  begin
    begin
      return;
    end;
  end;
  
  var := 1; -- Noncompliant
end;
/

-- correct
begin
  return;
end;
/

begin
  raise;
end;
/

begin
  raise_application_error(-20999, 'Custom error message');
end;
/

begin
  for i in 1..10 loop
      continue when i = 5;
      var := 1;
  end loop;
end;
/

begin
  for i in 1..10 loop
    exit when i = 5;
    var := 1;
  end loop;
end;
/

begin
  begin
    return;
  exception
    when others then
      null;
  end;
  
  var := 1;
end;
/

begin
  begin
    begin
      return;
    end;
  exception
    when others then
      null;
  end;
  
  var := 1; -- violation
end;
/
