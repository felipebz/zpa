declare
  e_custom exception;
begin
  raise no_data_found; -- Noncompliant {{Avoid raising the standard exception NO_DATA_FOUND.}}
  raise too_many_rows; -- Noncompliant {{Avoid raising the standard exception TOO_MANY_ROWS.}}
  raise e_custom;
  raise;
end;
