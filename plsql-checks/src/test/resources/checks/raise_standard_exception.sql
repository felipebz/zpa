declare
  e_custom exception;
begin
  raise no_data_found; -- violation
  raise too_many_rows; -- violation
  raise e_custom;
  raise;
end;