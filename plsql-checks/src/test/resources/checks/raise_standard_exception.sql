declare
  e_custom exception;
begin
  raise no_data_found; -- Noncompliant {{Avoid raising the standard exception no_data_found.}}
  raise too_many_rows; -- Noncompliant {{Avoid raising the standard exception too_many_rows.}}
  raise e_custom;
  raise;
end;