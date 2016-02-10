begin
  v := 0;
  null; -- Noncompliant {{This NULL statement does not have any effect here.}}
end;
/
begin
  null; -- Noncompliant
  v := 0;
end;
/

-- correct
begin
  null;
end;
/