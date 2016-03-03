declare
  i number;
begin
  -- a comment
  select count(*)
    into i
    from dual;
end;