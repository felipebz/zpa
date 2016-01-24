begin
  v := 0;
  null; -- violation
end;
/
begin
  null; -- violation
  v := 0;
end;
/

-- correct
begin
  null;
end;
/