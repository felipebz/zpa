begin
  declare -- Noncompliant {{Remove this DECLARE keyword.}}
  begin
    null;
  end;
  
  declare
    var number;
  begin
    null;
  end;
end;